package nl.nfi.cellscanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;

public class PreferencesActivity extends AppCompatActivity {
    public static class PreferenceFragment extends PreferenceFragmentCompat
    {
        private final PreferencesActivity a;

        public PreferenceFragment(PreferencesActivity a) {
            this.a = a;
        }

        private void setupRecording() {
            SwitchPreferenceCompat recording_switch = findPreference("APP_RECORDING");
            final SwitchPreferenceCompat gps_recording_switch = findPreference("GPS_RECORDING");
            final SwitchPreferenceCompat save_battery_switch = findPreference("save_battery");

            gps_recording_switch.setEnabled(recording_switch.isChecked());
            save_battery_switch.setEnabled(gps_recording_switch.isEnabled() && gps_recording_switch.isChecked());

            recording_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    gps_recording_switch.setEnabled((boolean)newValue);
                    save_battery_switch.setEnabled(gps_recording_switch.isEnabled() && gps_recording_switch.isChecked());
                    return true;
                }
            });

            gps_recording_switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    save_battery_switch.setEnabled((boolean)newValue);
                    return true;
                }
            });
        }

        private void setupSharing() {
            SwitchPreferenceCompat upload_switch = findPreference("auto_upload");
            final SwitchPreferenceCompat wifi_switch = findPreference("upload_on_wifi_only");

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
            Log.v("TESTA", "TESTA");
            setPreferencesFromResource(R.xml.preferences, rootKey);

            setupRecording();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        PreferenceFragment prefs = new PreferenceFragment(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, prefs)
                .commit();

        AppInfoActivity.showIfNoConsent(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppInfoActivity.showIfNoConsent(this);
    }
}
