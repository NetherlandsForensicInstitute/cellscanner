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
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        Intent i = getIntent();
        if (i != null && i.getData() != null)
            dialogApplySettings(i.getData());
    }

    private void dialogApplySettings(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getText(R.string.apply_settings_text))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    try {
                        applySettings(getContentResolver().openInputStream(uri));
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, "file not found", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {});
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void applySettings(InputStream is) {
        String data = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        JSONObject json = null;
        try {
            json = new JSONObject(data);
        } catch (JSONException e) {
            Toast.makeText(this, "failed to load configuration: "+e, Toast.LENGTH_LONG).show();
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        JSONArray names = json.names();
        if (names == null) {
            Toast.makeText(this, "unrecognized file format", Toast.LENGTH_LONG).show();
        } else {
            try {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    Object value = json.get(key);
                    if (!p.contains(key))
                        Toast.makeText(this, "new key: " + key, Toast.LENGTH_LONG).show();

                    if (value == null)
                        p.edit().putString(key, null).apply();
                    else if (value instanceof Boolean)
                        p.edit().putBoolean(key, (Boolean)value).apply();
                    else if (value instanceof String)
                        p.edit().putString(key, (String)value).apply();
                    else
                        Toast.makeText(this, String.format("unrecognized type for %s: %s", key, value.getClass().getName()), Toast.LENGTH_LONG).show();
                }

                prefs.setPreferenceScreen(null);
                prefs.addPreferencesFromResource(R.xml.preferences);

                Toast.makeText(this, "settings applied", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "failed to apply configuration: " + e, Toast.LENGTH_LONG).show();
            }
        }
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
