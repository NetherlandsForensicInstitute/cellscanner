package nl.nfi.cellscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasAccessCourseLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasFineLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;
import static nl.nfi.cellscanner.recorder.RecorderUtils.gpsHighPrecisionRecordingState;
import static nl.nfi.cellscanner.recorder.RecorderUtils.gpsRecordingState;
import static nl.nfi.cellscanner.recorder.RecorderUtils.inRecordingState;

public class PreferencesActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int PERMISSION_REQUEST_START_RECORDING = 1;

    private PreferenceFragment prefs;

    private SwitchPreferenceCompat swRecordingMaster;
    private SwitchPreferenceCompat swGPSRecord;
    private SwitchPreferenceCompat swGPSPrecision;

    public static class PreferenceFragment extends PreferenceFragmentCompat
    {
        private final PreferencesActivity a;

        public PreferenceFragment(PreferencesActivity a) {
            this.a = a;
        }

        private void setupSharing() {
            Preference view_measurements_button = findPreference("view_measurements");
            Preference share_data_button = findPreference("share_data");
            SwitchPreferenceCompat upload_switch = findPreference("auto_upload");
            final SwitchPreferenceCompat wifi_switch = findPreference("upload_on_wifi_only");

            view_measurements_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(a, ViewMeasurementsActivity.class);
                    a.startActivity(i);
                    return true;
                }
            });

            share_data_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    a.exportData();
                    return true;
                }
            });

            wifi_switch.setEnabled(upload_switch.isChecked());

            upload_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    wifi_switch.setEnabled((boolean)newValue);
                    return true;
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            a.setupRecording();
            setupSharing();

            findPreference("about_cellscanner").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoActivity.show(a);
                    return true;
                }
            });
        }
    }

    private void setupRecording() {
        swRecordingMaster = prefs.findPreference("APP_RECORDING");
        swGPSRecord = prefs.findPreference("GPS_RECORDING");
        swGPSPrecision = prefs.findPreference("gps_high_accuracy");

        swGPSRecord.setEnabled(swRecordingMaster.isChecked());
        swGPSPrecision.setEnabled(swGPSRecord.isEnabled() && swGPSRecord.isChecked());

        swRecordingMaster.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean recording_enabled = (boolean)newValue;

                if (recording_enabled)
                    requestStartRecording();
                else
                    RecorderUtils.stopService(PreferencesActivity.this);

                swGPSRecord.setEnabled(!recording_enabled);
                swGPSPrecision.setEnabled(!recording_enabled && swGPSRecord.isChecked());

                return true;
            }
        });

        swGPSRecord.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                swGPSPrecision.setEnabled((boolean)newValue);
                return true;
            }
        });

        swGPSPrecision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return true;
            }
        });

        toggleButtonsRecordingState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        prefs = new PreferenceFragment(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, prefs)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppInfoActivity.showIfNoConsent(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Set the buttons on the screen to recording state, or not recording state
     */
    private void toggleButtonsRecordingState() {
        boolean isInRecordingState = inRecordingState(this);
        swRecordingMaster.setChecked(isInRecordingState);

        swGPSRecord.setEnabled(!isInRecordingState);
        swGPSPrecision.setEnabled(!isInRecordingState);

        swGPSRecord.setChecked(gpsRecordingState(this));
        swGPSPrecision.setChecked(gpsHighPrecisionRecordingState(this));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        toggleButtonsRecordingState();
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
                    RecorderUtils.setRecordingState(this, false);

                    // explain the app will not be working
                    Toast.makeText(this, "App will not work without location permissions", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    public void requestStartRecording() {
        if (!hasAccessCourseLocationPermission(this))
            requestLocationPermission(this);
        else if (RecorderUtils.gpsRecordingState(this) && !hasFineLocationPermission(this))
            requestLocationPermission(this);
        else
            RecorderUtils.startService(this);
    }

    /**
     * Request permission to the end user for Location usage. Please be aware that this request is
     * done on a separate thread
     */
    private static void requestLocationPermission(Activity ctx) {
        ActivityCompat.requestPermissions(ctx, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_START_RECORDING);
    }

    /**
     * Export data via email
     */
    public void exportData() {
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
                        CellScannerApp.resetDatabase(getApplicationContext());
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
}
