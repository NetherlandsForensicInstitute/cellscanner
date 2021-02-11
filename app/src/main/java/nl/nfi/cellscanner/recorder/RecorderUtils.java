package nl.nfi.cellscanner.recorder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import nl.nfi.cellscanner.PreferencesActivity;


/**
 * Responsible for controlling the Reordering service
 *
 * Class gives access to the state of the recording service and allows for the user to start and
 * stop the service
 */
public class RecorderUtils {
    /**
     * Check the state of the Recording key.
     * @return State of the Recording key, when True the app should record cell data
     */
    public static boolean inRecordingState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_ENABLE, false);
    }

    /**
     * Check the state of the GPS Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data when
     *      the recording state is True
     */
    public static boolean gpsRecordingState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_GPS_RECORDING, true);
    }

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static boolean gpsHighPrecisionRecordingState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_GPS_HIGH_PRECISION_RECORDING, false);
    }

    /**
     * Set Recording to State value
     * When True, the app should record data
     * */
    public static void setRecordingState(Context context, Boolean state) { putBoolean(context, PreferencesActivity.PREF_ENABLE, state); };

    /**
     * Set GPS recording to State value
     * GPS Recording, when True, store GPS location when the app is recording cells
     * */
    public static void setGPSRecordingState(Context context, Boolean state) { putBoolean(context, PreferencesActivity.PREF_GPS_RECORDING, state); };

    public static void setGPSHighPrecisionRecordingState(Context context, Boolean state) { putBoolean(context, PreferencesActivity.PREF_GPS_HIGH_PRECISION_RECORDING, state); };


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

    public static boolean exportMeteredAllowed(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_UPLOAD_ON_WIFI_ONLY, false);
    }

    public static boolean autoDataUploadWanted(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferencesActivity.PREF_AUTO_UPLOAD, false);
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
        // TODO: toggling the state might be set to the onStart of the service. Android might stop it
        setRecordingState(context, true);
    }

    /**
     * Stops the service and sets the recording state to False
     * @param context: Context Stopping the service
     */
    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, LocationRecordingService.class);
        context.stopService(serviceIntent);
        // TODO: toggling the state might be set to the onDestroy of the service. Android might stop it
        setRecordingState(context, false);
    }
}