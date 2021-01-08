package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Responsible for controlling the recording functionality
 */
public class Recorder {
    // TODO: PICK A NAME NOT `MY_PREFS_NAME`
    final private static String PREFS_NAME = "MY_PREFS_NAME";

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