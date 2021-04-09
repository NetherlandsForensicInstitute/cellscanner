package nl.nfi.cellscanner;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import static nl.nfi.cellscanner.recorder.PermissionSupport.hasUserConsent;
import static nl.nfi.cellscanner.recorder.PermissionSupport.setUserConsent;

public class AppInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_and_conditions);

        final Context context = this;

        // Show the device identifier and app version
        StringBuffer dynamic_content = new StringBuffer();
        dynamic_content.append(String.format("Device identifier: %s\n", Preferences.getInstallID(context)));
        String version_name = CellScannerApp.getVersionName(context);
        if (version_name != null)
            dynamic_content.append(String.format("Cellscanner version: %s\n", version_name));
        TextView tv_dynamic = findViewById(R.id.tac_dynamic_content);
        tv_dynamic.setText(dynamic_content.toString());

        // Set textview with string containing HTML for URLs and phonenumbers
        String contactInformation = getString(R.string.tac_colophon);
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

        When consent has already been given, do not show the option
         */
        final CheckBox accepted_checkbox = findViewById(R.id.tac_checkbox);

        if (!userConsent) {
            accepted_checkbox.setChecked(userConsent);
            accepted_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setUserConsent(context, isChecked);
                    close_button.setEnabled(isChecked);  // Enabled if agreed to T & C
                }
            });
        } else {
            /* user already accepted, we assume the user came via the 'about entry point' */
            accepted_checkbox.setVisibility(View.INVISIBLE);
            ViewGroup.LayoutParams params = accepted_checkbox.getLayoutParams();
            params.height = 0;
            accepted_checkbox.setLayoutParams(params);

            /*
             * When the checkbox becomes invisible the old scrollTextView does not
             * relay its constraints to the bottom button of the screen. Following hack
             * relays the old bottom constraint from the checkbox to the
             * button
             * */
            View scrollTextView = findViewById(R.id.scrollView2);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) scrollTextView.getLayoutParams();
            layoutParams.bottomToTop = R.id.tac_close_button;
            scrollTextView.setLayoutParams(layoutParams);
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
