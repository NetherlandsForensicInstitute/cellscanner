package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import nl.nfi.cellscanner.Preferences;

import static nl.nfi.cellscanner.recorder.PermissionSupport.hasCourseLocationPermission;
import static nl.nfi.cellscanner.recorder.PermissionSupport.hasFineLocationPermission;


/**
 * Responsible for controlling the Reordering service
 *
 * Class gives access to the state of the recording service and allows for the user to start and
 * stop the service
 */
public class RecorderUtils {
    public static final int PERMISSION_REQUEST_START_RECORDING = 1;
    public final static int PERMISSION_REQUEST_PHONE_STATE = 2;

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    public static void requestStartRecording(Activity ctx) {
        List<String> missing_permissions = PermissionSupport.getMissingPermissions(ctx);
        if (missing_permissions.isEmpty())
            RecorderUtils.startService(ctx);
        else
            ActivityCompat.requestPermissions(ctx, missing_permissions.toArray(new String[]{}), PERMISSION_REQUEST_START_RECORDING);
    }

    /**
     * Check the state of the Recording key.
     * @return State of the Recording key, when True the app should record cell data
     */
    public static boolean isRecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_ENABLE, false);
    }

    /**
     * Starts the recording service
     *
     * Staring method is based on the API lvl the app is running on/
     * @param context: Context starting the service
     */
    public static void startService(Context context) {
        // after API-26 the service should be a Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, new Intent(context, LocationRecordingService.class));
        } else {
            context.startService(new Intent(context, LocationRecordingService.class));
        }
    }

    /**
     * Stops the service and sets the recording state to False
     * @param context: Context Stopping the service
     */
    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, LocationRecordingService.class);
        context.stopService(serviceIntent);
    }
}