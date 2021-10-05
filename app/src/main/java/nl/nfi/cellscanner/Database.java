package nl.nfi.cellscanner;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.LocationCollector;
import nl.nfi.cellscanner.collect.TrafficCollector;
import nl.nfi.cellscanner.collect.cellinfo.CellStatus;

public class Database {
    protected static final int VERSION = 4;

    private static final String META_VERSION_CODE = "version_code";
    private static final String META_INSTALL_ID = "install_id";

    private SQLiteDatabase db;

    public static String getFileTitle() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US);
        return String.format("%s_cellinfo.sqlite3", fmt.format(new Date()));
    }

    public static File getDataFile(Context ctx) {
        return new File(ctx.getExternalFilesDir(null), "cellinfo.sqlite3");
    }

    public Database(SQLiteDatabase db) {
        this.db = db;
    }

    private String getLatestCell(String subscription) {
        Cursor c = db.rawQuery("SELECT radio, mcc, mnc, area, cid FROM cellinfo WHERE subscription = ? ORDER BY date_end DESC LIMIT 1", new String[]{subscription});
        try {
            if (c.moveToNext()) {
                String radio = c.getString(0);
                int mcc = c.getInt(1);
                int mnc = c.getInt(2);
                int lac = c.getInt(3);
                int cid = c.getInt(4);
                return String.format("%s: %d-%d-%d-%d", radio, mcc, mnc, lac, cid);
            } else {
                return "none";
            }
        } finally {
            c.close();
        }
    }

    public String getUpdateStatus() {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        StringBuffer s = new StringBuffer();

        Cursor c = db.rawQuery("SELECT subscription, MIN(date_start), MAX(date_end), SUM(date_end - date_start) FROM cellinfo GROUP BY subscription", new String[]{});
        try {
            for (int i=0 ; c.moveToNext() ; i++) {
                String subscription = c.getString(0);
                long first = c.getLong(1);
                long last = c.getLong(2);
                long time_sum = c.getLong(3);

                long now = new Date().getTime();
                long coverage = 100 * time_sum / (now - first);
                if (i > 0)
                    s.append("<p/>");
                s.append(String.format("%s: %s<br/>", subscription, getLatestCell(subscription)));
                s.append(String.format("coverage: %d%% since %s<br/>", coverage, fmt.format(first)));
                s.append(String.format("updated: %s<br/>", fmt.format(last)));
            }

            if (s.length() == 0)
                return "No measurements.";
            else
                return s.toString();

        } finally {
            c.close();
        }
    }

    /**
     * Update the current cellular connection status.
     *
     * @param status the cellular connection status
     */
    public void updateCellStatus(String subscription, Date date_start, Date date_end, CellStatus status) {
        ContentValues update = new ContentValues();
        update.put("date_end", date_end.getTime());
        int nrows = db.update("cellinfo", update, "subscription = ? AND date_start = ?", new String[]{subscription, Long.toString(date_start.getTime())});
        if (nrows == 0) {
            ContentValues insert = status.getContentValues();
            insert.put("subscription", subscription);
            insert.put("date_start", date_start.getTime());
            insert.put("date_end", date_end.getTime());
            db.insert("cellinfo", null, insert);
            Log.v(CellscannerApp.TITLE, "new cell: "+insert.toString());
        }
    }

    protected String getMetaEntry(String name) {
        Cursor c = db.query("meta", new String[]{"value"}, "entry = ?", new String[]{"versionCode"}, null, null, null);
        try {
            if (!c.moveToNext())
                return null;

            return c.getString(0);
        } finally {
            c.close();
        }
    }

    protected void setMetaEntry(String name, String value) {
        ContentValues content = new ContentValues();
        content.put("entry", name);
        content.put("value", value);
        int n = db.update("meta", content, "entry = ?", new String[]{name});
        if (n == 0)
            db.insert("meta", null, content);
    }

    public void storeVersionCode(Context ctx)
    {
        PackageInfo pInfo = null;
        try {
            pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("cellscanner", "error", e);
            return;
        }

        int versionCode = pInfo.versionCode;

        setMetaEntry(META_VERSION_CODE, Integer.toString(versionCode));
    }

    protected int getVersionCode() {
        String versionCode = getMetaEntry(META_VERSION_CODE);
        if (versionCode == null)
            return -1;
        else
            return Integer.parseInt(versionCode);
    }

    public void updateSettings(Context context) {
        storeInstallID(context);
    }

    public void storeInstallID(Context ctx) {
        String install_id = Preferences.getInstallID(ctx);
        setMetaEntry(META_INSTALL_ID, install_id);
    }

    public static void storeMessage(Throwable e) {
        StringWriter msg = new StringWriter();
        PrintWriter writer = new PrintWriter(msg);
        e.printStackTrace(writer);
        storeMessage(msg.toString());
    }

    public static void storeMessage(String msg) {
        // message should not exceed maximum length
        if (msg.length() > 250)
            msg = msg.substring(0, 250);

        ContentValues content = new ContentValues();
        content.put("date", new Date().getTime());
        content.put("message", msg);
        CellscannerApp.getDatabaseConnection().insert("message", null, content);
    }

    public void storeLocationInfo(Location location) {
        // NOTE: some values (accuracy, speed, altitude) may not be available, but database version 2 and earlier has NOT NULL constraint
        ContentValues values = new ContentValues();
        values.put("provider", location.getProvider());
        values.put("timestamp", location.getTime());
        values.put("accuracy", location.getAccuracy());
        values.put("latitude", location.getLatitude());
        values.put("longitude", location.getLongitude());
        values.put("altitude", location.getAltitude());
        values.put("speed", location.getSpeed());
        if (location.hasBearing())
            values.put("bearing_deg", location.getBearing());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (location.hasSpeedAccuracy())
                values.put("speed_acc", location.getSpeedAccuracyMetersPerSecond());
            if (location.hasVerticalAccuracy())
                values.put("altitude_acc", location.getVerticalAccuracyMeters());
            if (location.hasBearingAccuracy())
                values.put("bearing_deg_acc", location.getBearingAccuracyDegrees());
        }

        db.insert("locationinfo", null, values);
    }

    public void storeCallState(int state) {
        String state_name;
        if (state == TelephonyManager.CALL_STATE_IDLE)
            state_name = "idle";
        else if (state == TelephonyManager.CALL_STATE_RINGING)
            state_name = "ringing";
        else if (state == TelephonyManager.CALL_STATE_OFFHOOK)
            state_name = "offhook";
        else
            state_name = "invalid";

        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("state", state_name);

        db.insert("call_state", null, values);
    }

    /** drop data until a given timestamp **/
    public void dropDataUntil(long timestamp) {
        db.delete("message", "date <= ?", new String[]{Long.toString(timestamp)});

        for (CollectorFactory f : Preferences.COLLECTORS.values())
            f.dropDataUntil(db, timestamp);

        db.execSQL("VACUUM");
    }

    public static void createTable(SQLiteDatabase db, String tab, String[] cols) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tab, TextUtils.join(",", cols)));
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meta ("+
                "  entry VARCHAR(200) NOT NULL PRIMARY KEY,"+
                "  value TEXT NOT NULL"+
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS message ("+
                "  date INT NOT NULL,"+
                "  message VARCHAR(250) NOT NULL"+
                ")");

        for (CollectorFactory f : Preferences.COLLECTORS.values())
            f.createTables(db);
    }

    public static void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (CollectorFactory f : Preferences.COLLECTORS.values())
            f.upgradeDatabase(db, oldVersion, newVersion);
    }
}
