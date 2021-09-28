package nl.nfi.cellscanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import nl.nfi.cellscanner.collect.RecorderUtils;

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
                prefs.setup();
                CellscannerApp.getDatabase().updateSettings(this);

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
                prefs.setRecordingEnabled(false);
                prefs.setCallStateRecording(false);
            }
        }

        // retry start service
        if (requestCode == RecorderUtils.PERMISSION_REQUEST_START_RECORDING) {
            RecorderUtils.applyRecordingPolicy(this);
        }
    }
}
