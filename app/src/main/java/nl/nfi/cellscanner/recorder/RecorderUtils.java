package nl.nfi.cellscanner.recorder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;


import androidx.core.content.ContextCompat;

import nl.nfi.cellscanner.CellScannerApp;



/**
 * Responsible for controlling the Reordering service
 *
 * Class gives access to the state of the recording service and allows for the user to start and
 * stop the service
 */
public class RecorderUtils {
    final private static String PREFS_NAME = CellScannerApp.TITLE;
    final private static String APP_RECORDING = "APP_RECORDING";  // APP should be recording data
    final private static String GPS_RECORDING = "GPS_RECORDING";  // APP should record GPS data when in Recording state
    final private static String GPS_HIGH_PRECISION_RECORDING = "GPS_HIGH_PRECISION_RECORDING";  // APP should record GPS data when in Recording state

    /**
     * Check the state of the Recording key.
     * @return State of the Recording key, when True the app should record cell data
     */
    public static boolean inRecordingState(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(APP_RECORDING, false);
    }

    /**
     * Check the state of the GPS Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data when
     *      the recording state is True
     */
    public static boolean gpsRecordingState(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(GPS_RECORDING, true);
    }

    /**
     * Check the state of the GPS HIGH precision Recording key.
     * @return State of the GPS Recording key, when True the app should record GPS data with HIGH precision when
     *      the recording state is True
     */
    public static boolean gpsHighPrecisionRecordingState(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(GPS_HIGH_PRECISION_RECORDING, false);
    }

    /**
     * Set Recording to State value
     * When True, the app should record data
     * */
    public static void setRecordingState(Context context, Boolean state) { putBoolean(context, APP_RECORDING, state); };

    /**
     * Set GPS recording to State value
     * GPS Recording, when True, store GPS location when the app is recording cells
     * */
    public static void setGPSRecordingState(Context context, Boolean state) { putBoolean(context, GPS_RECORDING, state); };

    public static void setGPSHighPrecisionRecordingState(Context context, Boolean state) { putBoolean(context, GPS_HIGH_PRECISION_RECORDING, state); };


    /**
     * Sets a boolean key to a given state in local storage
     *
     * @param context: Context making the request
     * @param target: Key to set
     * @param state: Boolean state to store
     */
    private static void putBoolean(Context context, String target, Boolean state ) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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