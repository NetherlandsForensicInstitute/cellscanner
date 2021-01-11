package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import cellscanner.wowtor.github.com.cellscanner.App;



/**
 * Responsible for controlling the recording functionality
 */
public class Recorder {
    final private static String PREFS_NAME = App.TITLE;

    public static boolean inRecordingState(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("RECORDING", false);
    }

    public static void setRecordingState(Context context, Boolean state) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("RECORDING", state);
        editor.apply();
    }

    public static void startService(Context context) {
        // after API-26 the service should be a Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, new Intent(context, LocationRecordingService.class));
        } else {
            context.startService(new Intent(context, LocationRecordingService.class));
        }
        setRecordingState(context, true);
    }

    public static void stopService(Context context) {
        // TODO: make this intent dependent on API LVL
        Intent serviceIntent = new Intent(context, LocationRecordingService.class);
        context.stopService(serviceIntent);
        setRecordingState(context, false);
    }
}