package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    public static void requestStartRecording(Activity ctx) {
        if (!hasCourseLocationPermission(ctx))
            requestLocationPermission(ctx);
        else if (isLocationRecordingEnabled(ctx) && !hasFineLocationPermission(ctx))
            requestLocationPermission(ctx);
        else
            RecorderUtils.startService(ctx);
    }

    /**
     * Request permission to the end user for Location usage. Please be aware that this request is
     * done on a separate thread
     */
    private static void requestLocationPermission(Activity ctx) {
        ActivityCompat.requestPermissions(ctx, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_START_RECORDING);
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
     * Check the state of the GPS Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data when
     *      the recording state is True
     */
    public static boolean isLocationRecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_GPS_RECORDING, true);
    }

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static boolean isHighPrecisionRecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_GPS_HIGH_PRECISION_RECORDING, false);
    }

    /**
     * Sets a boolean key to a given state in local storage
     *
     * @param context: Context making the request
     * @param target: Key to set
     * @param state: Boolean state to store
     */
    private static void putBoolean(Context context, String target, Boolean state ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(target, state);
        editor.apply();
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