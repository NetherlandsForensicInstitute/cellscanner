package nl.nfi.cellscanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.PermissionSupport;
import nl.nfi.cellscanner.recorder.Recorder;

import static androidx.core.content.FileProvider.getUriForFile;
import static nl.nfi.cellscanner.Database.getFileTitle;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;
import static nl.nfi.cellscanner.recorder.Recorder.inRecordingState;

public class MainActivity extends AppCompatActivity {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
     */

    private Button exportButton, clearButton;
    private SwitchCompat recorderSwitch;
    private TextView appStatus;


    private static final int PERMISSION_REQUEST_START_RECORDING = 1;

    /**
     * Fires when the system first creates the activity
     *
     * @param savedInstanceState: Bundle object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!hasUserConsent(this)) showTermsAndConditionsScreen();
        else showRecorderScreen();
    }

    private void showTermsAndConditionsScreen() {
        setContentView(nl.nfi.cellscanner.R.layout.terms_and_conditions);
        final Context context = this;
        boolean userConsent = hasUserConsent(context);

        final Button close_button = findViewById(R.id.tac_close_button);
        close_button.setEnabled(userConsent);

        /*
        Only allow (un)checking the consent the first time around. To retract consent, an email
        should be send asking to remove all data.
         */
        final CheckBox accepted_checkbox = findViewById(R.id.tac_checkbox);
        accepted_checkbox.setChecked(userConsent);
        if (!userConsent) {
            accepted_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setUserConsent(context, isChecked);
                    close_button.setEnabled(isChecked);  // Enabled if agreed to T & C
                }
            });
        } else {
            accepted_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    accepted_checkbox.setChecked(true);
                    Toast.makeText(context, "To revoke consent, contact the NFI to remove your data and then uninstall the app", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showRecorderScreen() {
        setContentView(nl.nfi.cellscanner.R.layout.activity_main);
        appStatus = findViewById(nl.nfi.cellscanner.R.id.userMessages);

        exportButton = findViewById(nl.nfi.cellscanner.R.id.exportButton);
        clearButton = findViewById(nl.nfi.cellscanner.R.id.clearButton);
        recorderSwitch = findViewById(nl.nfi.cellscanner.R.id.recorderSwitch);
        toggleButtonsRecordingState();

        /*
         Implement checked state listener on the switch that has the ability to start or stop the recording process
         */
        recorderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) requestStartRecording();
                else Recorder.stopService(getApplicationContext());
                toggleButtonsRecordingState();
            }
        });

        // run initial update to inform the end user
        updateLogViewer();

        // Setup listener for data updates, so the user can be informed
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateLogViewer();
                    }
                }, new IntentFilter(LocationRecordingService.LOCATION_DATA_UPDATE_BROADCAST)
        );
    }


    /**
     * Request permission to the end user for Location usage. Please be aware that this request is
     * done on a separate thread
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_START_RECORDING);
    }

    /**
     * Callback function for requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_START_RECORDING: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestStartRecording();
                } else {
                    // explain the app will not be working
                    Toast.makeText(this, "App will not work without location permissions", Toast.LENGTH_SHORT).show();
                    toggleButtonsRecordingState();
                }
            }
        }
    }


    /**
     * When closing terms and conditions screen, load the recorder screen
     *
     * @param view
     */
    public void closeTermsAndConditions(View view) {
        showRecorderScreen();
    }

    /**
     * Open the terms and conditions screen on clicking the Info button
     */
    public void openTermsAndConditions(View view) {
        showTermsAndConditionsScreen();
    }

    /**
     * Export data via email
     */
    public void exportData(View view) {
        if (!Database.getDataFile(this).exists())
            Toast.makeText(getApplicationContext(), "No database present.", Toast.LENGTH_SHORT).show();
        else {
            Uri contentUri = getUriForFile(this, ".fileprovider", Database.getDataFile(this));
            Intent sharingIntent = new Intent(Intent.ACTION_SENDTO);

            sharingIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
//            sharingIntent.putExtra(Intent.EXTRA_EMAIL, "email to send to ");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "datafile cell-scanner " + getFileTitle());
            sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

            if (sharingIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(sharingIntent);
            }
        }
    }


    public void clearDatabase(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Context ctx = getApplicationContext();
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        App.resetDatabase(getApplicationContext());
                        Toast.makeText(ctx, "database deleted", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(ctx, "pfew", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setMessage("Drop tables. Sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    public void requestStartRecording() {
        if (PermissionSupport.hasAccessCourseLocationPermission(this)) startRecording();
        else requestLocationPermission();
        toggleButtonsRecordingState();
    }

    /**
     * Start the recording service
     */
    private void startRecording() {
        Recorder.startService(this);
    }

    /**
     * Set the buttons on the screen to recording state, or not recording state
     */
    private void toggleButtonsRecordingState() {
        boolean isInRecordingState = inRecordingState(this);
        recorderSwitch.setChecked(isInRecordingState);
        exportButton.setEnabled(!isInRecordingState);
        clearButton.setEnabled(!isInRecordingState);
    }

    // todo: Reconnect with a timer or listener, to update every x =D
    private void updateLogViewer() {
        Database db = App.getDatabase();
        Log.v("UPDATE",db.getUpdateStatus());
        appStatus.setText(db.getUpdateStatus());
    }
}
