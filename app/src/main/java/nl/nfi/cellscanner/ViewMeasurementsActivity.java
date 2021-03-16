package nl.nfi.cellscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.nfi.cellscanner.recorder.LocationRecordingService;


public class ViewMeasurementsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    /*
    Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
    Communicate Activity <-> Service ... https://www.vogella.com/tutorials/AndroidServices/article.html
     */
    public static String RECORD_GPS = "1";  // field used for communicating

    private static final String TAG = ViewMeasurementsActivity.class.getSimpleName();

    // ui
    private TextView vlCILastUpdate, vlGPSLastUpdate, vlGPSProvider, vlGPSLat, vlGPSLon, vlGPSAcc,
            vlGPSAlt, vlGPSSpeed, vlUpLastUpdate, vlUpStatus, vlUpLastSuccess;

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setAutoUploadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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

        vlUpLastUpdate = findViewById(R.id.vlUpLastUpdate);
        vlUpStatus = findViewById(R.id.vlUpStatus);
        vlUpLastSuccess = findViewById(R.id.vlUpLastSuccess);

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

    private void setAutoUploadData() {
        vlUpStatus.setText(ExportResultRepository.getLastUploadMsg(getApplicationContext()));
        vlUpLastSuccess.setText(getDateTimeFromTimeStamp(ExportResultRepository.getLastSuccessfulUploadTimestamp(getApplicationContext()), "yyyy-MM-dd HH:mm:ss"));
        vlUpLastUpdate.setText(getDateTimeFromTimeStamp(ExportResultRepository.getLastUploadTimeStamp(getApplicationContext()),"yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        /*
        The last automatic upload data is stored as a shared preference.
        When this data changes, the data is retrieved and placed
        on the screen
         */
        Log.i("WORK", key);
        setAutoUploadData();
    }
}

