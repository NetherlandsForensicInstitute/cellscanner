package nl.nfi.cellscanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.gms.location.LocationRequest;

import java.net.URI;
import java.util.UUID;

import nl.nfi.cellscanner.collect.RecorderUtils;
import nl.nfi.cellscanner.upload.UploadUtils;

public class Preferences extends PreferenceFragmentCompat
{
    // recording preferences
    public final static String PREF_ENABLE = "APP_RECORDING";  // APP should be recording data
    public final static String PREF_CELLINFO_RECORDING = "CELLINFO_RECORDING";
    public final static String PREF_CELLINFO_METHOD = "CELLINFO_METHOD";
    public final static String PREF_CALL_STATE_RECORDING = "CALL_STATE_RECORDING";
    public final static String PREF_LOCATION_RECORDING = "LOCATION_RECORDING";  // APP should record GPS data when in Recording state
    public final static String PREF_LOCATION_ACCURACY = "LOCATION_ACCURACY";

    // data management preferences
    private static final String PREF_VIEW_MEASUREMENTS = "VIEW_MEASUREMENTS";
    private static final String PREF_START_UPLOAD = "START_UPLOAD";
    private static final String PREF_CLEAR_DATA = "CLEAR_DATA";
    private static final String PREF_AUTO_UPLOAD = "AUTO_UPLOAD";
    private static final String PREF_UPLOAD_URL = "UPLOAD_URL";
    private static final String PREF_UPLOAD_ON_WIFI_ONLY = "UPLOAD_ON_WIFI_ONLY";

    // general preferences
    private static final String PREF_INSTALL_ID = "INSTALL_ID";
    private final static String PREF_ABOUT_CELLSCANNER = "ABOUT_CELLSCANNER";

    private static final String PREF_MESSAGE_PUBLIC_KEY = "MESSAGE_PUBLIC_KEY";
    private static final String PREF_SSH_KNOWN_HOSTS = "SSH_KNOWN_HOSTS";

    public static final String[] COLLECTORS = new String[]{
            PREF_CELLINFO_RECORDING,
            PREF_CALL_STATE_RECORDING,
            PREF_LOCATION_RECORDING,
    };

    public static final String[] COLLECTOR_NAMES = new String[]{
            "cell data",
            "call state",
            "locations",
    };

    protected SwitchPreferenceCompat swRecordingMaster;
    protected SwitchPreferenceCompat swCellInfo;
    protected ListPreference swCellInfoMethod;
    protected SwitchPreferenceCompat swCallState;
    protected SwitchPreferenceCompat swGPSRecord;
    protected ListPreference swLocationAccuracy;

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
    public static boolean isRecordingEnabled(Context context, Intent intent) {
        if (!getBooleanPreference(context, intent, Preferences.PREF_ENABLE, true))
            return false;

        for (String collector : COLLECTORS) {
            if (isCollectorEnabled(collector, context, intent))
                return true;
        }

        return false;
    }

    public static boolean isRecordingEnabled(Context context) {
        return isRecordingEnabled(context, null);
    }

    public void setRecordingEnabled(boolean enabled) {
        if (swRecordingMaster != null) {
            swRecordingMaster.setChecked(enabled);
            swRecordingMaster.callChangeListener(enabled);
        }
    }

    private static boolean getBooleanPreference(Context context, Intent intent, String key, boolean default_value) {
        boolean value = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(key, default_value);

        if (intent != null)
            value = intent.getBooleanExtra(key, value);

        return value;
    }

    public static boolean isCollectorEnabled(String name, Context context, Intent intent) {
        return getBooleanPreference(context, intent, name, false);
    }

    public void setCollectorEnabled(String name, boolean enabled) {
        if (name.equals(PREF_CELLINFO_RECORDING)) {
            swCellInfo.setChecked(enabled);
            swCellInfo.callChangeListener(enabled);
        } else if (name.equals(PREF_CALL_STATE_RECORDING)) {
            swCallState.setChecked(enabled);
            swCallState.callChangeListener(enabled);
        } else if (name.equals(PREF_LOCATION_RECORDING)) {
            swGPSRecord.setChecked(enabled);
            swGPSRecord.callChangeListener(enabled);
        }
    }

    public static String getCellInfoMethod(Context context, Intent intent) {
        String value = null;
        if (intent != null)
            value = intent.getStringExtra(Preferences.PREF_CELLINFO_METHOD);
        if (value == null)
            value = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Preferences.PREF_CELLINFO_METHOD, null);
        if (value == null)
            value = "AUTO";

        return value;
    }

    /**
     * Check the state of the GPS Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data when
     *      the recording state is True
     */
    public static boolean isLocationRecordingEnabled(Context context, Intent intent) {
        return getBooleanPreference(context, intent, Preferences.PREF_LOCATION_RECORDING, false);
    }

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static int getLocationAccuracy(Context context, Intent intent) {
        String value = null;
        if (intent != null)
            value = intent.getStringExtra(Preferences.PREF_LOCATION_ACCURACY);
        if (value == null)
            value = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Preferences.PREF_LOCATION_ACCURACY, null);

        if (value != null) {
            if (value.equals("PASSIVE"))
                return LocationRequest.PRIORITY_NO_POWER;
            if (value.equals("LOW"))
                return LocationRequest.PRIORITY_LOW_POWER;
            if (value.equals("BALANCED"))
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            if (value.equals("HIGH"))
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
        }

        Log.e("cellscanner", "should not reach this code");
        return LocationRequest.PRIORITY_LOW_POWER;
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

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static String getMessagePublicKey(Context context) {
        String value = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Preferences.PREF_MESSAGE_PUBLIC_KEY, null);
        if (value != null && !value.equals(""))
            return value;
        else
            return null;
    }

    public static String getSshKnownHosts(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Preferences.PREF_SSH_KNOWN_HOSTS, null);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setup();
    }

    protected void setup() {
        setupRecording();
        setupSharing();

        findPreference(PREF_ABOUT_CELLSCANNER).setOnPreferenceClickListener(preference -> {
            AppInfoActivity.show(getContext());
            return true;
        });
    }

    private void setupRecording() {
        swRecordingMaster = findPreference(PREF_ENABLE);
        swCellInfo = findPreference(PREF_CELLINFO_RECORDING);
        swCellInfoMethod = findPreference(PREF_CELLINFO_METHOD);
        swCallState = findPreference(PREF_CALL_STATE_RECORDING);
        swGPSRecord = findPreference(PREF_LOCATION_RECORDING);
        swLocationAccuracy = findPreference(PREF_LOCATION_ACCURACY);

        swLocationAccuracy.setEnabled(swGPSRecord.isEnabled() && swGPSRecord.isChecked());

        swCellInfo.setEnabled(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY));
        swCellInfoMethod.setEnabled(swCellInfo.isEnabled() && swCellInfo.isChecked());

        swCallState.setEnabled(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY));

        if (swRecordingMaster != null) {
            swRecordingMaster.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (boolean) newValue;

                    Intent intent = new Intent();
                    intent.putExtra(PREF_ENABLE, enabled);
                    RecorderUtils.applyRecordingPolicy(getContext(), intent);

                    //swGPSRecord.setEnabled(enabled);
                    //swLocationAccuracy.setEnabled(swGPSRecord.isEnabled() && swGPSRecord.isChecked());
                    //swCallState.setEnabled(enabled);

                    return true;
                }
            });
        }

        swCellInfo.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean)newValue;

            Intent intent = new Intent();
            intent.putExtra(PREF_CELLINFO_RECORDING, enabled);
            RecorderUtils.applyRecordingPolicy(getContext(), intent);

            swCellInfoMethod.setEnabled(enabled);

            return true;
        });

        swCellInfoMethod.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.equals("TELEPHONY_CALLBACK") && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return false;
            }

            Intent intent = new Intent();
            intent.putExtra(PREF_CELLINFO_METHOD, (String)newValue);
            RecorderUtils.applyRecordingPolicy(getContext(), intent);

            return true;
        });

        swCallState.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean)newValue;

            Intent intent = new Intent();
            intent.putExtra(PREF_CALL_STATE_RECORDING, enabled);
            RecorderUtils.applyRecordingPolicy(getContext(), intent);

            return true;
        });

        swGPSRecord.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean)newValue;

            Intent intent = new Intent();
            intent.putExtra(PREF_LOCATION_RECORDING, enabled);
            RecorderUtils.applyRecordingPolicy(getContext(), intent);

            swLocationAccuracy.setEnabled(enabled);

            return true;
        });

        swLocationAccuracy.setOnPreferenceChangeListener((preference, newValue) -> {
            Intent intent = new Intent();
            intent.putExtra(PREF_LOCATION_ACCURACY, (String)newValue);
            RecorderUtils.applyRecordingPolicy(getContext(), intent);

            return true;
        });
    }

    private void setupSharing() {
        Preference view_measurements_button = findPreference(PREF_VIEW_MEASUREMENTS);
        final Preference start_upload_button = findPreference(PREF_START_UPLOAD);
        final Preference clear_data_button = findPreference(PREF_CLEAR_DATA);
        final SwitchPreferenceCompat upload_switch = findPreference(PREF_AUTO_UPLOAD);
        final EditTextPreference upload_server = findPreference(PREF_UPLOAD_URL);
        final SwitchPreferenceCompat wifi_switch = findPreference(PREF_UPLOAD_ON_WIFI_ONLY);

        view_measurements_button.setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(getContext(), ViewMeasurementsActivity.class);
            getContext().startActivity(i);
            return true;
        });

        start_upload_button.setOnPreferenceClickListener(preference -> {
            if (upload_server.getText().equals("")) {
                UploadUtils.exportData(Preferences.this.getContext());
            } else
                UserDataUploadWorker.startDataUpload(getContext());

            return true;
        });

        clear_data_button.setOnPreferenceClickListener(preference -> {
            clearDatabase();
            return true;
        });

        wifi_switch.setEnabled(upload_switch.isChecked());
        wifi_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                UserDataUploadWorker.applyUploadPolicy(getContext(), upload_switch.isChecked(), (boolean)newValue);
                return true;
            }
        });

        upload_switch.setEnabled(!upload_server.getText().equals(""));
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

                    upload_switch.setEnabled(false);
                    wifi_switch.setEnabled(false);

                    return true;
                } else {
                    // field set with new value
                    try {
                        URI new_uri = UploadUtils.validateURI((String)newValue);
                        Toast.makeText(getContext(), "Upload server: "+new_uri, Toast.LENGTH_LONG).show();

                        upload_switch.setEnabled(true);
                        wifi_switch.setEnabled(true);

                        return true;
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            }
        });

        wifi_switch.setEnabled(upload_switch.isChecked());
    }

    public void clearDatabase() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        CellscannerApp.resetDatabase(getContext());
                        Toast.makeText(getContext(), "database deleted", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(getContext(), "Clear database cancelled", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
        ab.setMessage("Drop tables. Sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }
}
