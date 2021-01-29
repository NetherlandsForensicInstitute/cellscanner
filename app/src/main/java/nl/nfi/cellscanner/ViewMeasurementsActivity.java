package nl.nfi.cellscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.AlphabeticIndex;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
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
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;

import static nl.nfi.cellscanner.recorder.RecorderUtils.gpsHighPrecisionRecordingState;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasAccessCourseLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasFineLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;
import static nl.nfi.cellscanner.recorder.RecorderUtils.gpsRecordingState;
import static nl.nfi.cellscanner.recorder.RecorderUtils.inRecordingState;


public class ViewMeasurementsActivity extends AppCompatActivity {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
    Communicate Activity <-> Service ... https://www.vogella.com/tutorials/AndroidServices/article.html
     */
    public static String RECORD_GPS = "1";  // field used for communicating

    private static final String TAG = ViewMeasurementsActivity.class.getSimpleName();

    // ui
    private TextView vlCILastUpdate, vlGPSLastUpdate, vlGPSProvider, vlGPSLat, vlGPSLon, vlGPSAcc, vlGPSAlt, vlGPSSpeed;

    /**
     * Fires when the system first creates the activity
     *
     * @param savedInstanceState: Bundle object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showRecorderScreen();
        AppInfoActivity.showIfNoConsent(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearGPSLocationFields();
        AppInfoActivity.showIfNoConsent(this);
    }

    private void showRecorderScreen() {
        setContentView(nl.nfi.cellscanner.R.layout.activity_view_measurements);

        vlCILastUpdate = findViewById(R.id.vlCILastUpdate);
        vlGPSLastUpdate = findViewById(R.id.vlGPSLastUpdate);
        vlGPSProvider = findViewById(R.id.vlGPSProvider);
        vlGPSLat = findViewById(R.id.vlGPSLat);
        vlGPSLon = findViewById(R.id.vlGPSLon);
        vlGPSAcc = findViewById(R.id.vlGPSAcc);
        vlGPSAlt = findViewById(R.id.vlGPSAlt);
        vlGPSSpeed = findViewById(R.id.vlGPSSpeed);

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

    // todo: Reconnect with a timer or listener, to update every x =D
    private void updateLogViewer() {
        updateLogViewer(null);
    }

    private void updateLogViewer(Intent intent) {
        Database db = CellScannerApp.getDatabase();
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
}

