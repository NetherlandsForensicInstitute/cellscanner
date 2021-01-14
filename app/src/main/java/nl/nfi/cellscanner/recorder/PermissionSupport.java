package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import nl.nfi.cellscanner.App;

public class PermissionSupport {
    final private static String PREFS_NAME = App.TITLE;

    public static boolean hasAccessCourseLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean hasFilePermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static boolean hasPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasUserConsent(final Context ctx) {
        boolean agreed = ctx
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("CONSENT", false);

        if (!agreed) {
            new AlertDialog.Builder(ctx)
                    .setTitle("License agreement")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("CONSENT", true);
                            editor.apply();
                        }
                    })
                    .setNegativeButton("No", null)
                    .setMessage("my pretty popup text!")
                    .show();
        }
        return ctx
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("CONSENT", false);
    }
}
