package nl.nfi.cellscanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nl.nfi.cellscanner.recorder.PermissionSupport;
import nl.nfi.cellscanner.recorder.Recorder;

import static nl.nfi.cellscanner.recorder.Recorder.inRecordingState;

public class MainActivity extends AppCompatActivity {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
     */

    private Button exportButton, clearButton;
    private Switch recorderSwitch;
    private static final int PERMISSION_REQUEST_START_RECORDING = 1;
    private static final int PERMISSION_REQUEST_EXPORT_DATA = 2;

    /**
     * Fires when the system first creates the activity
     * @param savedInstanceState: Bundle object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(nl.nfi.cellscanner.R.layout.activity_main);

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
                    Toast.makeText(this,"App will not work without location permissions", Toast.LENGTH_SHORT).show();
                    toggleButtonsRecordingState();
                }

                return;
            }
            case PERMISSION_REQUEST_EXPORT_DATA: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportData(null);
                }

                return;
            }
        }
    }

    private static String getFileTitle() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US);
        return String.format("%s_cellinfo.sqlite3", fmt.format(new Date()));
    }

    public void exportData(View view) {
//        THIS CODE IS BROKEN
//        if (requestFilePermission()) {
//            if (!Database.getDataPath(this).exists())
//            {
//                Toast.makeText(getApplicationContext(), "No database present.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.github.wowtor.cellscanner.fileprovider", Database.getDataPath(getApplicationContext()));
//
//            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
//            sharingIntent.setType("*/*");
//            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getFileTitle());
//            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
//            startActivity(Intent.createChooser(sharingIntent, "Share via"));
//        }
    }

    public void clearDatabase(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Context ctx = getApplicationContext();
                switch (which){
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

    public void requestStartRecording() {
        if (PermissionSupport.hasAccessCourseLocationPermission(this)) startRecording();
        else requestLocationPermission();
        toggleButtonsRecordingState();
    }

    private void startRecording() {
        Recorder.startService(this);
    }

    /**
     * Set the buttons on the screen to recording state, or not recording state
     * */
    private void toggleButtonsRecordingState() {
        boolean isInRecordingState = inRecordingState(this);
        recorderSwitch.setChecked(isInRecordingState);
        exportButton.setEnabled(!isInRecordingState);
        clearButton.setEnabled(!isInRecordingState);
    }

    // todo: Reconnect with a timer or listener, to update every x =D
    private void updateLogViewer() {
        TextView userMessages = findViewById(nl.nfi.cellscanner.R.id.userMessages);
        Database db = App.getDatabase();
        userMessages.setText(db.getUpdateStatus());
    }
}
