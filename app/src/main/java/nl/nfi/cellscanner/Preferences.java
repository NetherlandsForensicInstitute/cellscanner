package nl.nfi.cellscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import nl.nfi.cellscanner.recorder.PermissionSupport;
import nl.nfi.cellscanner.recorder.RecorderUtils;

public class Preferences extends PreferenceFragmentCompat
{
    // recording preferences
    public final static String PREF_ENABLE = "APP_RECORDING";  // APP should be recording data
    public final static String PREF_CALL_STATE_RECORDING = "CALL_STATE_RECORDING";
    public final static String PREF_GPS_RECORDING = "GPS_RECORDING";  // APP should record GPS data when in Recording state
    public final static String PREF_GPS_HIGH_PRECISION_RECORDING = "GPS_HIGH_ACCURACY";  // APP should record GPS data when in Recording state

    // data management preferences
    private static final String PREF_VIEW_MEASUREMENTS = "VIEW_MEASUREMENTS";
    private static final String PREF_SHARE_DATA = "SHARE_DATA";
    private static final String PREF_START_UPLOAD = "START_UPLOAD";
    private static final String PREF_AUTO_UPLOAD = "AUTO_UPLOAD";
    private static final String PREF_UPLOAD_URL = "UPLOAD_URL";
    private static final String PREF_UPLOAD_ON_WIFI_ONLY = "UPLOAD_ON_WIFI_ONLY";

    // general preferences
    private static final String PREF_INSTALL_ID = "INSTALL_ID";

    protected SwitchPreferenceCompat swRecordingMaster;
    protected SwitchPreferenceCompat swCallState;
    protected SwitchPreferenceCompat swGPSRecord;
    protected SwitchPreferenceCompat swGPSPrecision;

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

    /**
     * Check the state of the Recording key.
     * @return State of the Recording key, when True the app should record cell data
     */
    public static boolean isRecordingEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_ENABLE, false);
    }

    public static void setRecordingEnabled(Context context, boolean enabled) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(PREF_ENABLE, enabled).apply();
    }

    public static boolean isCallStateRecordingEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_CALL_STATE_RECORDING, false);
    }

    public static void setCallStateRecording(Context context, boolean enabled) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(PREF_CALL_STATE_RECORDING, enabled).apply();
    }

    /**
     * Check the state of the GPS Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data when
     *      the recording state is True
     */
    public static boolean isLocationRecordingEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_GPS_RECORDING, true);
    }

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static boolean isHighPrecisionRecordingEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_GPS_HIGH_PRECISION_RECORDING, false);
    }

    public static boolean getAutoUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_AUTO_UPLOAD, false);
    }

    public static String getUploadURL(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_UPLOAD_URL, null);
    }

    public static boolean getUnmeteredUploadOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_UPLOAD_ON_WIFI_ONLY, true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        setupRecording();
        setupSharing();
    }

    private void updateButtonEnabledState() {

    }

    private void setupRecording() {
        swRecordingMaster = findPreference(PREF_ENABLE);
        swCallState = findPreference(PREF_CALL_STATE_RECORDING);
        swGPSRecord = findPreference(PREF_GPS_RECORDING);
        swGPSPrecision = findPreference(PREF_GPS_HIGH_PRECISION_RECORDING);

        swCallState.setEnabled(!swRecordingMaster.isChecked());
        swGPSRecord.setEnabled(!swRecordingMaster.isChecked());
        swGPSPrecision.setEnabled(swGPSRecord.isEnabled() && swGPSRecord.isChecked());

        swRecordingMaster.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean recording_enabled = (boolean)newValue;
                Log.e("cellscanner", "checked changed "+recording_enabled);

                // update button states
                swCallState.setEnabled(!recording_enabled);
                swGPSRecord.setEnabled(!recording_enabled);
                swGPSPrecision.setEnabled(!recording_enabled && swGPSRecord.isChecked());

                // apply new settings
                if (recording_enabled) {
                    if (getContext() instanceof Activity)
                        RecorderUtils.requestStartRecording((Activity)getContext());
                    else
                        RecorderUtils.startService(getContext());
                } else {
                    RecorderUtils.stopService(getContext());
                }

                return true;
            }
        });

        swCallState.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (boolean)newValue;

                // request permission if necessary
                if (enabled && getContext() instanceof Activity && !PermissionSupport.hasCallStatePermission(getContext()))
                    ActivityCompat.requestPermissions((Activity)getContext(), new String[]{Manifest.permission.READ_PHONE_STATE}, RecorderUtils.PERMISSION_REQUEST_PHONE_STATE);

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
    }

    private void setupSharing() {
        Preference view_measurements_button = findPreference(PREF_VIEW_MEASUREMENTS);
        final Preference start_upload_button = findPreference(PREF_START_UPLOAD);
        final SwitchPreferenceCompat upload_switch = findPreference(PREF_AUTO_UPLOAD);
        final EditTextPreference upload_server = findPreference(PREF_UPLOAD_URL);
        final SwitchPreferenceCompat wifi_switch = findPreference(PREF_UPLOAD_ON_WIFI_ONLY);

        view_measurements_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getContext(), ViewMeasurementsActivity.class);
                getContext().startActivity(i);
                return true;
            }
        });

        start_upload_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserDataUploadWorker.startDataUpload(getContext());
                return true;
            }
        });

        wifi_switch.setEnabled(upload_switch.isChecked());

        wifi_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                UserDataUploadWorker.applyUploadPolicy(getContext(), upload_switch.isChecked(), (boolean)newValue);
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
                UserDataUploadWorker.applyUploadPolicy(getContext(), (boolean)newValue, wifi_switch.isChecked());
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
                    Toast.makeText(getContext(), "Upload server removed", Toast.LENGTH_LONG).show();
                    return true;
                } else {
                    // field set with new value
                    try {
                        URI url = new URI((String)newValue);
                        if (url.getScheme() == null) {
                            Toast.makeText(getContext(), "Protocol missing; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                            return false;
                        }
                        else if (url.getHost() == null) {
                            Toast.makeText(getContext(), "Host missing; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                            return false;
                        }
                        else if (url.getPath() != null && !url.getPath().equals("")) {
                            Toast.makeText(getContext(), "Upload path not supported; try a URL without a path", Toast.LENGTH_LONG).show();
                            return false;
                        }
                        else if (UserDataUploadWorker.getSupportedProtocols().contains(url.getScheme())) {
                            Toast.makeText(getContext(), "Server updated", Toast.LENGTH_LONG).show();
                            return true;
                        } else {
                            Toast.makeText(getContext(), "Unsupported protocol: " + url.getScheme(), Toast.LENGTH_LONG).show();
                            return false;
                        }
                    } catch (URISyntaxException e) {
                        Toast.makeText(getContext(), "Invalid input; try a valid URL such as sftp://user@hostname", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            }
        });

        wifi_switch.setEnabled(upload_switch.isChecked());
    }
}
