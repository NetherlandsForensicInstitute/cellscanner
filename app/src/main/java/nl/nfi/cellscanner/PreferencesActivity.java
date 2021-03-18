package nl.nfi.cellscanner;

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
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;

public class PreferencesActivity
        extends AppCompatActivity
{

    private Preferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        prefs = new Preferences();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, prefs)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                    prefs.setRecordingEnabled(false);

                    // explain the app will not be working
                    Toast.makeText(this, "App will not work without location permissions", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Export data via email (unused but may be useful)
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

    /**
     * Deletes the database (unused)
     *
     * @param view
     */
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
