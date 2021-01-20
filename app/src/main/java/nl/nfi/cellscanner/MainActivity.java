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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.Recorder;

import static nl.nfi.cellscanner.Database.getFileTitle;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasAccessCourseLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasFineLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;
import static nl.nfi.cellscanner.recorder.Recorder.gpsRecordingState;
import static nl.nfi.cellscanner.recorder.Recorder.inRecordingState;

public class MainActivity extends AppCompatActivity {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
    Communicate Activity <-> Service ... https://www.vogella.com/tutorials/AndroidServices/article.html
     */
    public static String RECORD_GPS = "1";  // field used for communicating

    private Button exportButton, clearButton;
    private SwitchCompat swRecordingMaster, swGPSRecord;
    private TextView vlCILastUpdate, vlGPSLastUpdate, vlGPSProvider, vlGPSLat, vlGPSLon, vlGPSAcc, vlGPSAlt, vlGPSSpeed;

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

        // Show the android ID to the user
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        TextView tvAndroidId = findViewById(R.id.tac_android_id_text);
        tvAndroidId.setText(androidId);

        // Set textview with string containing HTML for URLs and phonenumbers
        String contactInformation = getString(R.string.tac_contact_information);
        String dataCollection = getString(R.string.tac_data_collection);

        TextView tvContactInfo = findViewById(R.id.tac_contactInformation_text);
        TextView tvDataCollection = findViewById(R.id.tac_dataCollected_text);

        setHtmlVersionDependent(contactInformation, tvContactInfo);
        setHtmlVersionDependent(dataCollection, tvDataCollection);

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

    private void setHtmlVersionDependent(String htmlString, TextView tvObject) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvObject.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvObject.setText(Html.fromHtml(htmlString));
        }
    }

    private void showRecorderScreen() {
        setContentView(nl.nfi.cellscanner.R.layout.activity_main);

        vlCILastUpdate = findViewById(R.id.vlCILastUpdate);
        vlGPSLastUpdate = findViewById(R.id.vlGPSLastUpdate);
        vlGPSProvider = findViewById(R.id.vlGPSProvider);
        vlGPSLat = findViewById(R.id.vlGPSLat);
        vlGPSLon = findViewById(R.id.vlGPSLon);
        vlGPSAcc = findViewById(R.id.vlGPSAcc);
        vlGPSAlt = findViewById(R.id.vlGPSAlt);
        vlGPSSpeed = findViewById(R.id.vlGPSSpeed);

        exportButton = findViewById(nl.nfi.cellscanner.R.id.exportButton);
        clearButton = findViewById(nl.nfi.cellscanner.R.id.clearButton);
        swRecordingMaster = findViewById(nl.nfi.cellscanner.R.id.recorderSwitch);
        swGPSRecord = findViewById(R.id.swGPSRecord);

        /*
         Implement checked state listener on the switch that has the ability to start or stop the recording process
         */
        swRecordingMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) requestStartRecording();
                else Recorder.stopService(getApplicationContext());
                toggleButtonsRecordingState();
            }
        });

        swGPSRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Recorder.setGPSRecordingState(getApplicationContext(), isChecked);
                sendRecordGPSBroadcastMessage();
            }
        });


        toggleButtonsRecordingState();

        // run initial update to inform the end user
        updateLogViewer();

        // Setup listener for data updates, so the user can be informed
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateLogViewer(intent);
                    }
                }, new IntentFilter(LocationRecordingService.LOCATION_DATA_UPDATE_BROADCAST)
        );
    }


    /**
     * Request permission to the end user for Location usage. Please be aware that this request is
     * done on a separate thread
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_START_RECORDING);
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
        if (!Database.getDataFile(this).exists()) {
            Toast.makeText(getApplicationContext(), "No database present.", Toast.LENGTH_SHORT).show();

        } else {
            String[] TO = {""};

            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "nl.nfi.cellscanner.fileprovider", Database.getDataFile(getApplicationContext()));

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);

            sharingIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getFileTitle());
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

            //need this to prompts email client only
            sharingIntent.setDataAndType(uri, "message/rfc822");

            startActivity(Intent.createChooser(sharingIntent, "Share via"));
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
                        clearGPSLocationFields();
                        Toast.makeText(ctx, "database deleted", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(ctx, "Clear database cancelled", Toast.LENGTH_SHORT).show();
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
        if (hasAccessCourseLocationPermission(this) &&
                hasFineLocationPermission(this)) {
            startRecording();
        } else {
            requestLocationPermission();
        }
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
        swRecordingMaster.setChecked(isInRecordingState);
        exportButton.setEnabled(!isInRecordingState);
        clearButton.setEnabled(!isInRecordingState);

        swGPSRecord.setChecked(gpsRecordingState(this));
    }

    // todo: Reconnect with a timer or listener, to update every x =D
    private void updateLogViewer() {
        updateLogViewer(null);
    }

    private void updateLogViewer(Intent intent) {
        Database db = App.getDatabase();
        vlCILastUpdate.setText(db.getUpdateStatus());

        if (intent != null) {
            Bundle a = intent.getExtras();
            if (a != null && a.getBoolean("hasLoc", false)) {
                vlGPSLastUpdate.setText(getDateTimeFromTimeStamp(a.getLong("lts"), "yyyy-MM-dd HH:mm:ss"));
                vlGPSProvider.setText(String.valueOf(a.getString("pro")));
                vlGPSLat.setText(String.valueOf(a.getDouble("lat")));
                vlGPSLon.setText(String.valueOf(a.getDouble("lon")));
                vlGPSAcc.setText(String.valueOf(a.getFloat("acc")));
                vlGPSAlt.setText(String.valueOf(a.getDouble("alt")));
                vlGPSSpeed.setText(String.valueOf(a.getFloat("spd")));
            }
        }
    }

    private void clearGPSLocationFields() {
        vlGPSLastUpdate.setText(R.string.valueBaseText);
        vlGPSProvider.setText(R.string.valueBaseText);
        vlGPSLat.setText(R.string.valueBaseText);
        vlGPSLon.setText(R.string.valueBaseText);
        vlGPSAcc.setText(R.string.valueBaseText);
        vlGPSAlt.setText(R.string.valueBaseText);
        vlGPSSpeed.setText(R.string.valueBaseText);
    }

    private static String getDateTimeFromTimeStamp(Long time, String requestedDateFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(requestedDateFormat);
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date dateTime = new Date(time);
        return dateFormat.format(dateTime);
    }

    private void sendRecordGPSBroadcastMessage() {
        Intent intent = new Intent(RECORD_GPS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}

