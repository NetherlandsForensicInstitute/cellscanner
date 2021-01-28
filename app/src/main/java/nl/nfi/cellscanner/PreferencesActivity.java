package nl.nfi.cellscanner;

import android.content.Context;
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
                    a.showTermsAndConditionsScreen();
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
    }

    private void showTermsAndConditionsScreen() {
        setContentView(nl.nfi.cellscanner.R.layout.terms_and_conditions);
        final Context context = this;
        boolean userConsent = hasUserConsent(context);

        final Button close_button = findViewById(R.id.tac_close_button);
        close_button.setEnabled(userConsent);

        /*
        Only allow (un)checking the consent the first time around. To retract consent, an email
        should be send asking to remove all data.
         */
        final CheckBox accepted_checkbox = findViewById(R.id.tac_checkbox);
        accepted_checkbox.setChecked(userConsent);
        if (!userConsent) {
            accepted_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setUserConsent(context, isChecked);
                    close_button.setEnabled(isChecked);  // Enabled if agreed to T & C
                }
            });
        } else {
            accepted_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    accepted_checkbox.setChecked(true);
                    Toast.makeText(context, "To revoke consent, contact the NFI to remove your data and then uninstall the app", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
