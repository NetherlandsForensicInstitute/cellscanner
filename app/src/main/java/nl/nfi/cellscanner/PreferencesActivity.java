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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasAccessCourseLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasFineLocationPermission;
import static nl.nfi.cellscanner.recorder.RecorderUtils.exportMeteredAllowed;

public class PreferencesActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int PERMISSION_REQUEST_START_RECORDING = 1;

    private static final String PREF_INSTALL_ID = "INSTALL_ID";
    public final static String PREF_ENABLE = "APP_RECORDING";  // APP should be recording data
    public final static String PREF_GPS_RECORDING = "GPS_RECORDING";  // APP should record GPS data when in Recording state
    public final static String PREF_GPS_HIGH_PRECISION_RECORDING = "GPS_HIGH_ACCURACY";  // APP should record GPS data when in Recording state

    private static final String PREF_VIEW_MEASUREMENTS = "VIEW_MEASUREMENTS";
    private static final String PREF_SHARE_DATA = "SHARE_DATA";
    public static final String PREF_AUTO_UPLOAD = "AUTO_UPLOAD";
    public static final String PREF_UPLOAD_ON_WIFI_ONLY = "UPLOAD_ON_WIFI_ONLY";

    private PreferenceFragment prefs;

    private SwitchPreferenceCompat swRecordingMaster;
    private SwitchPreferenceCompat swGPSRecord;
    private SwitchPreferenceCompat swGPSPrecision;

    /**
     * Returns a unique identifier (UUID) for this Cellscanner setup. The value should be the same for
     * subsequent calls to this. The following actions will cause the identifier to be re-generated:
     *
     * - the app is re-installed
     * - all Cellscanner-related app data are cleared from the Android settings menu.
     *
     * The output is a UUID generated randomly once after installation, and then stored with the app
     * settings.
     *
     * @param ctx the application context
     * @return a UUID of this Cellscanner setup.
     */
    public static String getInstallID(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String install_id = prefs.getString(PREF_INSTALL_ID, null);

        if (install_id == null) {
            // install id not yet available; generate one randomly
            install_id = UUID.randomUUID().toString();
            prefs.edit().putString(PREF_INSTALL_ID, install_id).apply();
        }

        return install_id;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat
    {
        private final PreferencesActivity preferencesActivity;

        public PreferenceFragment(PreferencesActivity a) {
            this.preferencesActivity = a;
        }

        private void setupSharing() {
            Preference view_measurements_button = findPreference(PREF_VIEW_MEASUREMENTS);
            Preference share_data_button = findPreference(PREF_SHARE_DATA);
            final SwitchPreferenceCompat upload_switch = findPreference(PREF_AUTO_UPLOAD);
            final SwitchPreferenceCompat wifi_switch = findPreference(PREF_UPLOAD_ON_WIFI_ONLY);

            view_measurements_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(preferencesActivity, ViewMeasurementsActivity.class);
                    preferencesActivity.startActivity(i);
                    return true;
                }
            });

            share_data_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preferencesActivity.exportData();
                    return true;
                }
            });

            wifi_switch.setEnabled(upload_switch.isChecked());

            wifi_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (RecorderUtils.autoDataUploadWanted(getContext())) {
                        // there is need for auto data upload
                        preferencesActivity.unSchedulePeriodDataUpload();
                        preferencesActivity.schedulePeriodicDataUpload();
                    }
                    return true;
                }
            });

            upload_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // toggle the scheduled upload of data
                    if ((boolean)newValue) {
                        preferencesActivity.schedulePeriodicDataUpload();
                    } else {
                        preferencesActivity.unSchedulePeriodDataUpload();
                    }

                    wifi_switch.setEnabled((boolean)newValue);
                    return true;
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            preferencesActivity.setupRecording();
            setupSharing();

            findPreference("about_cellscanner").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoActivity.show(preferencesActivity);
                    return true;
                }
            });
        }
    }

    private void setupRecording() {
        swRecordingMaster = prefs.findPreference(PREF_ENABLE);
        swGPSRecord = prefs.findPreference(PREF_GPS_RECORDING);
        swGPSPrecision = prefs.findPreference(PREF_GPS_HIGH_PRECISION_RECORDING);

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

        updateButtonStateToRecordingState();
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
    private void updateButtonStateToRecordingState() {
        boolean isInRecordingState = RecorderUtils.inRecordingState(this);
        swRecordingMaster.setChecked(isInRecordingState);

        swGPSRecord.setEnabled(!isInRecordingState);
        swGPSRecord.setChecked(RecorderUtils.gpsRecordingState(this));

        swGPSPrecision.setEnabled(!isInRecordingState && swGPSRecord.isChecked());
        swGPSPrecision.setChecked(RecorderUtils.gpsHighPrecisionRecordingState(this));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // TODO, this should be removed, as the switches are not externally controlled
        updateButtonStateToRecordingState();
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


    @NotNull
    private Constraints getWorkManagerConstraints() {
        NetworkType networkType = exportMeteredAllowed(this) ? NetworkType.UNMETERED : NetworkType.CONNECTED;
        return new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();
    }

    /**
     * Schedules a Periodic Upload of the data
     */
    private void schedulePeriodicDataUpload() {
        Constraints constraints = getWorkManagerConstraints();

        PeriodicWorkRequest uploadWorkRequest = new PeriodicWorkRequest
                .Builder(UserDataUploadWorker.class, 15, TimeUnit.MINUTES) // TODO: Make this a useful setting
                .addTag(UserDataUploadWorker.TAG)
                .setConstraints(constraints)
                .build();

        WorkManager
                .getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(UserDataUploadWorker.TAG, ExistingPeriodicWorkPolicy.REPLACE, uploadWorkRequest);
    }

    public void unSchedulePeriodDataUpload() {
        WorkManager
                .getInstance(getApplicationContext())
                .cancelAllWorkByTag(UserDataUploadWorker.TAG);
    }

}
