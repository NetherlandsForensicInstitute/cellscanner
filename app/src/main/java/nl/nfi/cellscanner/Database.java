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

    private Long getLongFromSQL(String query) {
        Cursor c = db.rawQuery(query, new String[]{});
        try {
            c.moveToNext();
            if (c.isNull(0)) {
                return null;
            } else {
                return c.getLong(0);
            }
        } finally {
            c.close();
        }
    }

    private String getCurrentCell(String subscription) {
        Date now = new Date();
        Cursor c = db.rawQuery("SELECT radio, mcc, mnc, area, cid FROM cellinfo WHERE subscription = ? AND date_end > ? ORDER BY date_end DESC LIMIT 1", new String[]{subscription, Long.toString(now.getTime() - 60000)});
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

    private Date getTimestampFromSQL(String query) {
        Long v = getLongFromSQL(query);
        return v == null ? null : new Date(v);
    }

    public String getLocationUpdateStatus() {
        Cursor c = db.rawQuery("SELECT MIN(timestamp), COUNT(*) FROM locationinfo", new String[]{});
        try {
            c.moveToNext();
            long first = c.getLong(0);
            int count = c.getInt(1);
            long now = new Date().getTime();
            if (count > 0 && now > first)
                return String.format("coverage: %d measurements since %d minutes\n", count, (now - first) / 1000 / 60);
            else
                return null;
        } finally {
            c.close();
        }
    }

    public String getUpdateStatus() {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        StringBuffer s = new StringBuffer();

        Cursor c = db.rawQuery("SELECT subscription, MIN(date_start), MAX(date_end), SUM(date_end - date_start) FROM cellinfo GROUP BY subscription", new String[]{});
        try {
            for (;c.moveToNext();) {
                String subscription = c.getString(0);
                long first = c.getLong(1);
                long last = c.getLong(2);
                long time_sum = c.getLong(3);

                long now = new Date().getTime();
                long coverage = 100 * time_sum / (now - first);
                s.append(String.format("current cell %s: %s\n", subscription, getCurrentCell(subscription)));
                s.append(String.format("updated: %s\n", fmt.format(last)));
                s.append(String.format("coverage: %d%% since %s\n", coverage, fmt.format(first)));
                s.append("\n");
            }

            if (s.length() == 0)
                return "no data";
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
            Log.v(CellScannerApp.TITLE, "new cell: "+insert.toString());
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

    public void storeMessage(Throwable e) {
        StringWriter msg = new StringWriter();
        PrintWriter writer = new PrintWriter(msg);
        e.printStackTrace(writer);
        storeMessage(msg.toString());
    }

    public void storeMessage(String msg) {
        // message should not exceed maximum length
        if (msg.length() > 250)
            msg = msg.substring(0, 250);

        ContentValues content = new ContentValues();
        content.put("date", new Date().getTime());
        content.put("message", msg);
        db.insert("message", null, content);
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
        db.delete("cellinfo", "date_end <= ?", new String[]{Long.toString(timestamp)});
        db.delete("locationinfo", "timestamp <= ?", new String[]{Long.toString(timestamp)});
        db.execSQL("VACUUM");
    }


    protected static void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: remove NOT NULL constraint to some columns of `locationinfo` (applies to database version 2 and earlier)

        if (oldVersion < 2) {
            // no upgrade for versions prior to 2
            createTables(db);
            return;
        }

        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE locationinfo ADD COLUMN altitude_acc INT");
            db.execSQL("ALTER TABLE locationinfo ADD COLUMN speed_acc INT");
            db.execSQL("ALTER TABLE locationinfo ADD COLUMN bearing_deg INT");
            db.execSQL("ALTER TABLE locationinfo ADD COLUMN bearing_deg_acc INT");
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE cellinfo ADD COLUMN subscription VARCHAR(20) NOT NULL DEFAULT 'unknown'");
        }
    }

    private static void createTable(SQLiteDatabase db, String tab, String[] cols) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tab, TextUtils.join(",", cols)));
    }

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meta ("+
                "  entry VARCHAR(200) NOT NULL PRIMARY KEY,"+
                "  value TEXT NOT NULL"+
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS message ("+
                "  date INT NOT NULL,"+
                "  message VARCHAR(250) NOT NULL"+
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS call_state ("+
                "  date INT NOT NULL,"+
                "  state VARCHAR(20) NOT NULL"+
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS cellinfo ("+
                "  subscription VARCHAR(20) NOT NULL,"+
                "  date_start INT NOT NULL,"+
                "  date_end INT NOT NULL,"+
                "  registered INT NOT NULL,"+
                "  radio VARCHAR(10) NOT NULL,"+ // radio technology (GSM, UMTS, LTE)
                "  mcc INT NOT NULL,"+
                "  mnc INT NOT NULL,"+
                "  area INT NOT NULL,"+ // Location Area Code (GSM, UMTS) or TAC (LTE)
                "  cid INT NOT NULL,"+ // Cell Identity (GSM: 16 bit; LTE: 28 bit)
                "  bsic INT,"+ // Base Station Identity Code (GSM only)
                "  arfcn INT,"+ // Absolute RF Channel Number (GSM only)
                "  psc INT,"+ // 9-bit UMTS Primary Scrambling Code described in TS 25.331 (UMTS only/)
                "  uarfcn INT,"+ // 16-bit UMTS Absolute RF Channel Number (UMTS only)
                "  pci INT"+ // Physical Cell Id 0..503, Integer.MAX_VALUE if unknown (LTE only)
                ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS cellinfo_date_end ON cellinfo(date_end)");

        createTable(db, "locationinfo", new String[]{
            "provider VARCHAR(200)",
            "accuracy INT",  // accuracy in meters
            "timestamp INT NOT NULL",
            "latitude INT NOT NULL",
            "longitude INT NOT NULL",
            "altitude INT",  // altitude in meters
            "altitude_acc INT",  // altitude accuracy in meters (available in Oreo and up)
            "speed INT",  // speed in meters per second
            "speed_acc INT",  // speed accuracy in meters per second (available in Oreo and up)
            "bearing_deg INT",  // bearing in degrees
            "bearing_deg_acc INT",  // bearing accuracy in degrees (available in Oreo and up)
        });
    }
}
