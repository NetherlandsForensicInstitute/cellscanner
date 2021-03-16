package nl.nfi.cellscanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.service.autofill.UserData;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;

public class PreferencesActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // recording preferences
    public final static String PREF_ENABLE = "APP_RECORDING";  // APP should be recording data
    public final static String PREF_GPS_RECORDING = "GPS_RECORDING";  // APP should record GPS data when in Recording state
    public final static String PREF_GPS_HIGH_PRECISION_RECORDING = "GPS_HIGH_ACCURACY";  // APP should record GPS data when in Recording state

    // data management preferences
    private static final String PREF_VIEW_MEASUREMENTS = "VIEW_MEASUREMENTS";
    private static final String PREF_SHARE_DATA = "SHARE_DATA";
    private static final String PREF_START_UPLOAD = "START_UPLOAD";
    private static final String PREF_AUTO_UPLOAD = "AUTO_UPLOAD";
    private static final String PREF_UPLOAD_URL = "UPLOAD_URL";
    public static final String PREF_UPLOAD_ON_WIFI_ONLY = "UPLOAD_ON_WIFI_ONLY";

    private static final String PREF_INSTALL_ID = "INSTALL_ID";

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
            Preference start_upload_button = findPreference(PREF_START_UPLOAD);
            final SwitchPreferenceCompat upload_switch = findPreference(PREF_AUTO_UPLOAD);
            final EditTextPreference upload_server = findPreference(PREF_UPLOAD_URL);
            final SwitchPreferenceCompat wifi_switch = findPreference(PREF_UPLOAD_ON_WIFI_ONLY);

            view_measurements_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(preferencesActivity, ViewMeasurementsActivity.class);
                    preferencesActivity.startActivity(i);
                    return true;
                }
            });

            start_upload_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    UserDataUploadWorker.startDataUpload(preferencesActivity);
                    return true;
                }
            });

            upload_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean upload_enabled = (boolean) newValue;
                    if (upload_enabled && upload_server.getText().equals(""))
                        return false;

                    wifi_switch.setEnabled(upload_enabled);
                    UserDataUploadWorker.applyUploadPolicy(preferencesActivity, (boolean)newValue, wifi_switch.isChecked());
                    return true;
                }
            });

            upload_server.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.equals(upload_server.getText())) {
                        // no change
                        return true;
                    } else if (newValue.equals("")) {
                        // field cleared
                        if (upload_switch.isChecked())
                            upload_switch.setChecked(false);
                        Toast.makeText(preferencesActivity, "Upload server removed", Toast.LENGTH_LONG).show();
                        return true;
                    } else {
                        // field set with new value
                        try {
                            URI url = new URI((String)newValue);
                            if (url.getScheme() == null) {
                                Toast.makeText(preferencesActivity, "Protocol missing; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                                return false;
                            }
                            else if (url.getHost() == null) {
                                Toast.makeText(preferencesActivity, "Host missing; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                                return false;
                            }
                            else if (url.getPath() != null && !url.getPath().equals("")) {
                                Toast.makeText(preferencesActivity, "Upload path not supported; try a URL without a path", Toast.LENGTH_LONG).show();
                                return false;
                            }
                            else if (UserDataUploadWorker.getSupportedProtocols().contains(url.getScheme())) {
                                Toast.makeText(preferencesActivity, "Server updated", Toast.LENGTH_LONG).show();
                                return true;
                            } else {
                                Toast.makeText(preferencesActivity, "Unsupported protocol: " + url.getScheme(), Toast.LENGTH_LONG).show();
                                return false;
                            }
                        } catch (URISyntaxException e) {
                            Toast.makeText(preferencesActivity, "Invalid input; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                }
            });

            wifi_switch.setEnabled(upload_switch.isChecked());
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

                // update button states
                swGPSRecord.setEnabled(!recording_enabled);
                swGPSPrecision.setEnabled(!recording_enabled && swGPSRecord.isChecked());

                // apply new settings
                if (recording_enabled)
                    RecorderUtils.requestStartRecording(PreferencesActivity.this);
                else
                    RecorderUtils.stopService(PreferencesActivity.this);

                return true;
            }
        });

        swGPSRecord.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // update button state
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

        // resume foreground service if necessary
        if (RecorderUtils.isRecordingEnabled(this))
            RecorderUtils.requestStartRecording(this);
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
        boolean isInRecordingState = RecorderUtils.isRecordingEnabled(this);
        swRecordingMaster.setChecked(isInRecordingState);

        swGPSRecord.setEnabled(!isInRecordingState);
        swGPSRecord.setChecked(RecorderUtils.isLocationRecordingEnabled(this));

        swGPSPrecision.setEnabled(!isInRecordingState && swGPSRecord.isChecked());
        swGPSPrecision.setChecked(RecorderUtils.isHighPrecisionRecordingEnabled(this));
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
            case RecorderUtils.PERMISSION_REQUEST_START_RECORDING: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    RecorderUtils.requestStartRecording(this);
                } else {
                    swRecordingMaster.setChecked(false);

                    // explain the app will not be working
                    Toast.makeText(this, "App will not work without location permissions", Toast.LENGTH_SHORT).show();
                }
            }
        }
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

    public static boolean getAutoUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_AUTO_UPLOAD, false);
    }

    public static String getUploadURL(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PreferencesActivity.PREF_UPLOAD_URL, null);
    }

    public static boolean getUnmeteredUploadOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_UPLOAD_ON_WIFI_ONLY, true);
    }
}
