package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.content.Context;
import android.content.SharedPreferences;


public class Recorder {
    final private static String PREFS_NAME = "MY_PREFS_NAME";

    public static boolean inRecordingState(Context context) {
        // TODO: PICK A NAME NOT `MY_PREFS_NAME`
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