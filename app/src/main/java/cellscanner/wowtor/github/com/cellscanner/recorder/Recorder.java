package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.content.Context;


public class Recorder {
    public static boolean inRecordingState(Context context) {
        // TODO: PICK A NAME NOT `MY_PREFS_NAME`
        return context
                .getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE)
                .getBoolean("RECORDING", false);
    }
}