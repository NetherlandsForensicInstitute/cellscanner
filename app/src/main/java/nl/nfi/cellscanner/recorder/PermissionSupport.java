package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import nl.nfi.cellscanner.App;

public class PermissionSupport {
    final private static String PREFS_NAME = App.TITLE;

    public static boolean hasAccessCourseLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean hasFineLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean hasFilePermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static boolean hasPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns whether user has given consent to terms and conditions
     * @param ctx main activity Context
     * @return boolean
     */
    public static boolean hasUserConsent(final Context ctx) {
        return ctx
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("CONSENT", false);
    }

    /**
     * Set the user's consent
     * @param ctx main activity
     * @param consentGiven boolean reflecting user's consent
     */
    public static void setUserConsent(final Context ctx, boolean consentGiven) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("CONSENT", consentGiven);
        editor.apply();
    }
}
