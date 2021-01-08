package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

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
        Log.i("RECORDER", "START");
        // after API-26 the service should be a Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startAPI26UpService(context);
        } else {
            // TODO: SHOULD BE IMPLEMENTED
        }
    }

    public static void stopService(Context context) {
        Log.i("RECORDER", "STOP");
        // TODO: make this intent dependent on API LVL
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        context.stopService(serviceIntent);
        setRecordingState(context, false);
    }

    private static void startAPI26UpService(Context context) {
        Log.i("RECORDER", "starting api 26+ version begin");
        ContextCompat.startForegroundService(context, new Intent(context, ForegroundService.class));
        setRecordingState(context, true);
        Log.i("RECORDER", "starting api 26+ version end");
    }

}