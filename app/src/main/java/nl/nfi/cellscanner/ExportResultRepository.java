package nl.nfi.cellscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class ExportResultRepository {
    public static final String AUTO = "automated";
    public static final String MANUAL = "manual";


    private static final String LAST_UPLOAD_SUCCESSFUL = "LAST_UPLOAD_SUCCESSFUL";
    private static final String LAST_SUCCESSFUL_UPLOAD_TIMESTAMP = "LAST_SUCCESSFUL_UPLOAD_TIMESTAMP";
    private static final String LAST_UPLOAD_TIMESTAMP = "LAST_UPLOAD_TIMESTAMP";
    private static final String LAST_UPLOAD_MSSG = "LAST_UPLOAD_MSSG";
    private static final String LAST_UPLOAD_TYPE = "LAST_UPLOAD_TYPE";


    public static void storeExportResult(Context ctx, long timestamp, boolean uploadSuccessful, String uploadMessage, String uploadType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(LAST_UPLOAD_SUCCESSFUL, uploadSuccessful);
        editor.putLong(LAST_UPLOAD_TIMESTAMP, timestamp);
        editor.putString(LAST_UPLOAD_MSSG, uploadMessage);
        editor.putString(LAST_UPLOAD_TYPE, uploadType);

        if (uploadSuccessful) {
            editor.putLong(LAST_SUCCESSFUL_UPLOAD_TIMESTAMP, timestamp);
        }

        editor.apply();

    }
}