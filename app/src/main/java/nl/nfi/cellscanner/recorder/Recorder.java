package nl.nfi.cellscanner.recorder;

import android.content.Context;
import android.content.SharedPreferences;

import nl.nfi.cellscanner.App;


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

}