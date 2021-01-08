package cellscanner.wowtor.github.com.cellscanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
     */

    private Button exportButton, clearButton;
    private Switch recorderSwitch;
    private static final int PERMISSION_REQUEST_START_RECORDING = 1;
    private static final int PERMISSION_REQUEST_EXPORT_DATA = 2;
    //private static final int CDMA_COORDINATE_DIVISOR = 3600 * 4;

    /**
     * Fires when the system first creates the activity
     * @param savedInstanceState: Bundle object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(cellscanner.wowtor.github.com.cellscanner.R.layout.activity_main);

        exportButton = findViewById(cellscanner.wowtor.github.com.cellscanner.R.id.exportButton);
        clearButton = findViewById(cellscanner.wowtor.github.com.cellscanner.R.id.clearButton);
        recorderSwitch = findViewById(cellscanner.wowtor.github.com.cellscanner.R.id.recorderSwitch);

        recorderSwitch.setChecked(LocationService.isRunning());
        exportButton.setEnabled(!LocationService.isRunning());
        clearButton.setEnabled(!LocationService.isRunning());

        recorderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportButton.setEnabled(!isChecked);
                clearButton.setEnabled(!isChecked);
                if (isChecked)
                    startRecording();
                else
                    stopRecording();
            }
        });

        Toast.makeText(getApplicationContext(), String.format("Cellscanner service is %srunning.", LocationService.isRunning() ? "" : "not "), Toast.LENGTH_SHORT).show();

        final Handler handler = new Handler();
        Runnable timer = new Runnable() {
            @Override
            public void run() {
                updateLogViewer();;
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timer);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * User returns, app was already started
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Starts when the activity is no longer visible
     * */
    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_START_RECORDING);
            return false;
        }
    }

    private boolean requestFilePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_EXPORT_DATA);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_START_RECORDING: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    startRecording();
                } else {
                    // permission denied
                    LocationService.stop(getApplicationContext());
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
        if (requestFilePermission()) {
            if (!Database.getDataPath(this).exists())
            {
                Toast.makeText(getApplicationContext(), "No database present.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.github.wowtor.cellscanner.fileprovider", Database.getDataPath(getApplicationContext()));

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("*/*");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getFileTitle());
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        }
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

    public void startRecording() {
        Context ctx = getApplicationContext();
        if (requestLocationPermission()) {
            LocationService.start(this);
            Toast.makeText(ctx, "Location service started", Toast.LENGTH_SHORT).show();
            Log.v(App.TITLE, "Location service started");
        } else {
            Toast.makeText(ctx, "no permission -- try again", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopRecording() {
        LocationService.stop(this);
        Context ctx = getApplicationContext();
        Toast.makeText(ctx, "Location service stopped", Toast.LENGTH_SHORT).show();
        Log.v(App.TITLE, "Location service stopped");
        exportButton.setEnabled(true);
        clearButton.setEnabled(true);
    }

    private void updateLogViewer() {
        TextView userMessages = findViewById(cellscanner.wowtor.github.com.cellscanner.R.id.userMessages);
        Database db = App.getDatabase();
        userMessages.setText(db.getUpdateStatus());
    }
}
