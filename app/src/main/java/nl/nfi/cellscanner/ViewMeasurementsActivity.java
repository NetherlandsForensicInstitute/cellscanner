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
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import nl.nfi.cellscanner.collect.CollectorFactory;


public class ViewMeasurementsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String REFRESH_BROADCAST = "REFRESH_BROADCAST_MESSAGE";

    /*
        Activity lifecycle, see: https://developer.android.com/guide/components/activities/activity-lifecycle
        Communicate Activity <-> Service ... https://www.vogella.com/tutorials/AndroidServices/article.html
         */
    // ui
    private TextView status_view;
    private TextView upload_status;

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

        status_view = findViewById(R.id.status_text);
        upload_status = findViewById(R.id.upload_status_content);

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
        StringBuffer status = new StringBuffer();
        for (Map.Entry<String, CollectorFactory> collector : Preferences.COLLECTORS.entrySet()) {
            String enabled_text = (Preferences.isRecordingEnabled(this) && Preferences.isCollectorEnabled(collector.getKey(), this, null)) ? "enabled" : "disabled";
            status.append("<p>");
            status.append(String.format("<b>%s</b>: %s<br/>", collector.getValue().getTitle(), enabled_text));
            status.append(collector.getValue().getStatusText());
            status.append("</p>");
        }

        status_view.setText(HtmlCompat.fromHtml(status.toString(), 0));
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

        upload_status.setText(statustext);
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

    public static void refresh(Context ctx) {
        Intent intent = new Intent(ViewMeasurementsActivity.REFRESH_BROADCAST);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }
}

