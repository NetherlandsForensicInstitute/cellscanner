package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;


public class PermissionSupport {

    public static boolean hasAccessCourseLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean hasFineLocationPermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
    }
    
    private static boolean hasPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
