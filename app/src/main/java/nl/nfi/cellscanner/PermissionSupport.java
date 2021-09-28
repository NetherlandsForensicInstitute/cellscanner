package nl.nfi.cellscanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class used to check if permissions are set
 */
public class PermissionSupport {
    final private static String PREFS_NAME = CellscannerApp.TITLE;

    public static List<String> getMissingPermissions(Context ctx, Intent extra) {
        List<String> missing_permissions = new ArrayList<String>();

        if (!PermissionSupport.hasCourseLocationPermission(ctx))
            missing_permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!PermissionSupport.hasFineLocationPermission(ctx))
            missing_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (!PermissionSupport.hasCallStatePermission(ctx))
            missing_permissions.add(Manifest.permission.READ_PHONE_STATE);

        return missing_permissions;
    }

    public static boolean hasCourseLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean hasFineLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean hasCallStatePermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.READ_PHONE_STATE);
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
