package nl.nfi.cellscanner;

import android.Manifest;
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

import nl.nfi.cellscanner.recorder.PermissionSupport;
import nl.nfi.cellscanner.recorder.RecorderUtils;

import static nl.nfi.cellscanner.Database.getFileTitle;

public class PreferencesActivity
        extends AppCompatActivity
{
    public Preferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        if (prefs == null)
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
        RecorderUtils.applyRecordingPolicy(this);
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
        // update switches on denied requests
        for (int i=0 ; i< permissions.length ; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                prefs.setRecordingEnabled(false);
            }
            if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                prefs.setCallStateRecording(false);
            }
        }

        // retry start service
        if (requestCode == RecorderUtils.PERMISSION_REQUEST_START_RECORDING) {
            RecorderUtils.applyRecordingPolicy(this);
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
