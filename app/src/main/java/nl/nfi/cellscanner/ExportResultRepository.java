package nl.nfi.cellscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class ExportResultRepository {
    public static final String AUTO = "automated";
    public static final String MANUAL = "manual";

    public static final String LAST_UPLOAD_SUCCESSFUL = "LAST_UPLOAD_SUCCESSFUL";
    public static final String LAST_SUCCESSFUL_UPLOAD_TIMESTAMP = "LAST_SUCCESSFUL_UPLOAD_TIMESTAMP";
    public static final String LAST_UPLOAD_TIMESTAMP = "LAST_UPLOAD_TIMESTAMP";
    public static final String LAST_UPLOAD_MSG = "LAST_UPLOAD_MSSG";
    public static final String LAST_UPLOAD_TYPE = "LAST_UPLOAD_TYPE";


    public static void storeExportResult(Context ctx, long timestamp, boolean uploadSuccessful, String uploadMessage, String uploadType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(LAST_UPLOAD_SUCCESSFUL, uploadSuccessful);
        editor.putLong(LAST_UPLOAD_TIMESTAMP, timestamp);
        editor.putString(LAST_UPLOAD_MSG, uploadMessage);
        editor.putString(LAST_UPLOAD_TYPE, uploadType);

        if (uploadSuccessful) {
            editor.putLong(LAST_SUCCESSFUL_UPLOAD_TIMESTAMP, timestamp);
        }

        editor.commit();
    }

    public static long getLastUploadTimeStamp(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getLong(LAST_UPLOAD_TIMESTAMP, 0);
    }

    public static long getLastSuccessfulUploadTimestamp(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getLong(LAST_SUCCESSFUL_UPLOAD_TIMESTAMP, 0);
    }

    public static boolean getLastUploadSuccessful(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(LAST_UPLOAD_SUCCESSFUL, false);
    }

    public static String getLastUploadType(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(LAST_UPLOAD_TYPE, "UNKNOWN_ERROR");
    }


    public static String getLastUploadMsg(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(LAST_UPLOAD_MSG, "UNKNOWN_ERROR");
    }

}