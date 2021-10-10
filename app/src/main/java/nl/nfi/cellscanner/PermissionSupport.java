package nl.nfi.cellscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import nl.nfi.cellscanner.collect.DataCollector;

/**
 * Utility class used to check if permissions are set
 */
public class PermissionSupport {
    final private static String PREFS_NAME = CellscannerApp.TITLE;

    public static List<String> getMissingPermissions(Context ctx, Intent extra) {
        List<String> missing_permissions = new ArrayList<String>();

        for (String name : Preferences.COLLECTORS.keySet()) {
            if (Preferences.isRecordingEnabled(ctx, extra) && Preferences.isCollectorEnabled(name, ctx, extra)) {
                DataCollector collector = Preferences.createCollector(name, ctx);
                for (String perm : collector.requiredPermissions(extra)) {
                    if (!hasPermission(ctx, perm)) {
                        missing_permissions.add(perm);
                    }
                }
            }
        }

        return missing_permissions;
    }

    public static boolean hasPermissions(Context ctx, String[] permissions) {
        for (String perm : permissions) {
            if (!hasPermission(ctx, perm))
                return false;
        }

        return true;
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
