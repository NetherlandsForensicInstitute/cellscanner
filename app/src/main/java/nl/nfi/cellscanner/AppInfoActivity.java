package nl.nfi.cellscanner;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;

public class AppInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_and_conditions);

        final Context context = this;

        // Show the android ID to the user
        String installId = PreferencesActivity.getInstallID(context);
        TextView tvAndroidId = findViewById(R.id.tac_install_id_text);
        tvAndroidId.setText(installId);

        // Set textview with string containing HTML for URLs and phonenumbers
        String contactInformation = getString(R.string.tac_contact_information);
        String dataCollection = getString(R.string.tac_data_collection);
        String aboutApp = getString(R.string.tac_about_app);

        TextView tvContactInfo = findViewById(R.id.tac_contactInformation_text);
        TextView tvDataCollection = findViewById(R.id.tac_dataCollected_text);
        TextView tvAboutApp = findViewById(R.id.tac_aboutApp_text);

        setHtmlVersionDependent(contactInformation, tvContactInfo);
        setHtmlVersionDependent(dataCollection, tvDataCollection);
        setHtmlVersionDependent(aboutApp, tvAboutApp);

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

    private void setHtmlVersionDependent(String htmlString, TextView tvObject) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvObject.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvObject.setText(Html.fromHtml(htmlString));
        }
    }

    /**
     * When closing terms and conditions screen, load the recorder screen
     *
     * @param view
     */
    public void closeTermsAndConditions(View view) {
        finish();
    }

    protected static void show(Context ctx) {
        Intent i = new Intent(ctx, AppInfoActivity.class);
        ctx.startActivity(i);
    }

    protected static void showIfNoConsent(Context ctx) {
        if (!hasUserConsent(ctx)) {
            Intent i = new Intent(ctx, AppInfoActivity.class);
            ctx.startActivity(i);
        }
    }
}
