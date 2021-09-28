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

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;


public class ViewMeasurementsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String REFRESH_BROADCAST = "REFRESH_BROADCAST_MESSAGE";

    /*
        Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
        Communicate Activity <-> Service ... https://www.vogella.com/tutorials/AndroidServices/article.html
         */
    // ui
    private TextView vlCILastUpdate, vl_location_status, vl_upload_status;

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
        updateLogViewer();
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

        vlCILastUpdate = findViewById(R.id.cell_status);
        vl_location_status = findViewById(R.id.location_status);
        vl_upload_status = findViewById(R.id.upload_status_content);

        // run initial update to inform the end user
        updateLogViewer();

        // Setup listener for data updates, so the user can be informed
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateLogViewer();
                    }
                }, new IntentFilter(REFRESH_BROADCAST)
        );
    }

    private void updateLogViewer() {
        Database db = CellscannerApp.getDatabase();
        StringBuffer ci_status = new StringBuffer();
        ci_status.append(String.format("recording: %s\n\n", Preferences.isRecordingEnabled(getApplicationContext()) ? "enabled" : "disabled"));
        ci_status.append(db.getUpdateStatus());
        vlCILastUpdate.setText(ci_status);

        StringBuffer location_status = new StringBuffer();
        location_status.append(String.format("recording: %s\n\n", Preferences.isLocationRecordingEnabled(getApplicationContext(), null) ? "enabled" : "disabled"));
        location_status.append(db.getLocationUpdateStatus());
        vl_location_status.setText(location_status);
    }

    private static String getDateTimeFromTimeStamp(Long time) {
        if (time == 0)
            return "never";
        else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            dateFormat.setTimeZone(TimeZone.getDefault());
            Date dateTime = new Date(time);
            return dateFormat.format(dateTime);
        }
    }

    private void setAutoUploadData() {
        Context context = getApplicationContext();

        String upload_message = ExportResultRepository.getLastUploadMsg(context);
        long last_update_timestamp = ExportResultRepository.getLastUploadTimeStamp(context);
        long last_success_timestamp = ExportResultRepository.getLastSuccessfulUploadTimestamp(context);

        StringBuffer statustext = new StringBuffer();
        statustext.append(String.format("periodic upload: %s\n", Preferences.getAutoUploadEnabled(context) ? Preferences.getUploadURL(context) : "disabled"));
        if (Preferences.getAutoUploadEnabled(context))
            statustext.append(String.format("upload format: %s\n", Preferences.getMessagePublicKey(context) != null ? "sqlite3.aes.gz" : "sqlite3.gz"));

        statustext.append("last upload: " + getDateTimeFromTimeStamp(last_success_timestamp) + "\n");

        if (last_update_timestamp != last_success_timestamp) {
            statustext.append("last attempt: " + getDateTimeFromTimeStamp(last_update_timestamp) + "\n");
            statustext.append("message: " + upload_message + "\n");
        }

        // Show the device identifier and app version
        statustext.append(String.format("device identifier: %s\n", Preferences.getInstallID(context)));
        String version_name = CellscannerApp.getVersionName(context);
        if (version_name != null)
            statustext.append(String.format("cellscanner version: %s\n", version_name));

        vl_upload_status.setText(statustext);
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

