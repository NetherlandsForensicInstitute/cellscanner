package nl.nfi.cellscanner;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Database {
    protected static final int VERSION = 2;

    private static final String META_VERSION_CODE = "version_code";
    private static final String META_ANDROID_ID = "android_id";

    private SQLiteDatabase db;
    private Date previous_date = null;

    public static File getDataPath(Context ctx) {
        return new File(ctx.getExternalFilesDir(null), "cellinfo.sqlite3");
    }

    public Database(SQLiteDatabase db) {
        this.db = db;
    }

    private Long getLongFromSQL(String query) {
        Cursor c = db.rawQuery(query, new String[]{});
        c.moveToNext();
        if (c.isNull(0)) {
            return null;
        } else {
            return c.getLong(0);
        }
    }

    private String[] getActiveCells(Date date) {
        List<String> cells = new ArrayList<String>();
        if (date != null) {
            Cursor c = db.rawQuery("SELECT radio, mcc, mnc, area, cid FROM cellinfo WHERE ? BETWEEN date_start AND date_end", new String[]{Long.toString(date.getTime())});
            while (c.moveToNext()) {
                String radio = c.getString(0);
                int mcc = c.getInt(1);
                int mnc = c.getInt(2);
                int lac = c.getInt(3);
                int cid = c.getInt(4);
                cells.add(String.format("%s: %d-%d-%d-%d", radio, mcc, mnc, lac, cid));
            }
        }

        return cells.toArray(new String[]{});
    }

    private Date getTimestampFromSQL(String query) {
        Long v = getLongFromSQL(query);
        return v == null ? null : new Date(v);
    }

    public String getUpdateStatus() {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        Date last_update_time = getTimestampFromSQL("SELECT MAX(date_end) FROM cellinfo");
        String[] current_cells = getActiveCells(last_update_time);

        StringBuffer s = new StringBuffer();
        s.append(String.format("updated: %s\n", last_update_time == null ? "never" : fmt.format(last_update_time)));
        for (String cell: current_cells) {
            s.append(String.format("current cell: %s\n", cell));
        }

        return s.toString();
    }

    public boolean isValid(CellInfo info) {
        // TODO: improve by example https://github.com/zamojski/TowerCollector/tree/master/app/src/main/java/info/zamojski/soft/towercollector/collector/validators
        return info.isRegistered();
    }

    public String[] storeCellInfo(List<CellInfo> lst) {
        Date date = new Date();

        List<String> cells = new ArrayList<String>();
        for (CellInfo info : lst) {
            if (isValid(info))
                cells.add(storeCellInfo(date, info));
        }

        previous_date = date;
        return cells.toArray(new String[0]);
    }

    public String storeCellInfo(Date date, CellInfo info) {
        if (info instanceof CellInfoGsm) {
            return storeCellInfoGsm(date, (CellInfoGsm)info);
        } else if (info instanceof CellInfoCdma) {
            return storeCellInfoCdma(date, (CellInfoCdma)info);
        } else if (info instanceof CellInfoWcdma) {
            return storeCellInfoWcdma(date, (CellInfoWcdma)info);
        } else if (info instanceof CellInfoLte) {
            return storeCellInfoLte(date, (CellInfoLte) info);
        } else {
            Log.w(App.TITLE, "Unrecognized cell info object");
            return "unrecognized";
        }
    }

    private void updateCellInfo(String table, Date date, ContentValues values) {
        boolean contiguous = previous_date != null && date.getTime() < previous_date.getTime() + App.EVENT_VALIDITY_MILLIS;

        // if the previous registration has the same values, update the end time only
        if (contiguous) {
            ContentValues update = new ContentValues();
            update.put("date_end", date.getTime());

            ArrayList<String> qwhere = new ArrayList<String>();
            ArrayList<String> qargs = new ArrayList<String>();
            qwhere.add("date_end = ?");
            qargs.add(Long.toString(previous_date.getTime()));
            for (String key : values.keySet()) {
                qwhere.add(String.format("%s = ?", key));
                qargs.add(values.getAsString(key));
            }

            int nrows = db.update(table, update, TextUtils.join(" AND ", qwhere), qargs.toArray(new String[0]));
            if (nrows > 0)
                return;
        }

        // if there is no previous registration to be updated, create a new one
        ContentValues insert = new ContentValues(values);
        insert.put("date_start", date.getTime());
        insert.put("date_end", date.getTime());
        Log.v(App.TITLE, "new cell: "+insert);
        db.insert(table, null, insert);
    }

    private static String toString(CellInfoGsm info) {
        return String.format("%sGSM: %d-%d-%d-%d", info.isRegistered() ? "" : "unregistered: ", info.getCellIdentity().getMcc(), info.getCellIdentity().getMnc(), info.getCellIdentity().getLac(), info.getCellIdentity().getCid());
    }

    private static String toString(CellInfoCdma info) {
        return String.format("%scdma:%d-%d", info.isRegistered() ? "" : "unregistered: ", info.getCellIdentity().getBasestationId(), info.getCellIdentity().getNetworkId());
    }

    private static String toString(CellInfoWcdma info) {
        return String.format("%sUMTS: %d-%d-%d-%d", info.isRegistered() ? "" : "unregistered: ", info.getCellIdentity().getMcc(), info.getCellIdentity().getMnc(), info.getCellIdentity().getLac(), info.getCellIdentity().getCid());
    }

    private static String toString(CellInfoLte info) {
        return String.format("%sLTE: %d-%d-%d-%d", info.isRegistered() ? "" : "unregistered: ", info.getCellIdentity().getMcc(), info.getCellIdentity().getMnc(), info.getCellIdentity().getTac(), info.getCellIdentity().getCi());
    }

    private String storeCellInfoGsm(Date date, CellInfoGsm info) {
        ContentValues content = new ContentValues();
        content.put("registered", info.isRegistered() ? 1 : 0);
        content.put("radio", "GSM");
        content.put("mcc", info.getCellIdentity().getMcc());
        content.put("mnc", info.getCellIdentity().getMnc());
        content.put("area", info.getCellIdentity().getLac());
        content.put("cid", info.getCellIdentity().getCid());
        content.put("bsic", info.getCellIdentity().getBsic());
        content.put("arfcn", info.getCellIdentity().getArfcn());
        updateCellInfo("cellinfo", date, content);

        return toString(info);
    }

    private String storeCellInfoCdma(Date date, CellInfoCdma info) {
        ContentValues content = new ContentValues();
        content.put("registered", info.isRegistered() ? 1 : 0);
        content.put("basestationid", info.getCellIdentity().getBasestationId());
        content.put("latitude", info.getCellIdentity().getLatitude());
        content.put("longitude", info.getCellIdentity().getLongitude());
        content.put("networkid", info.getCellIdentity().getNetworkId());
        content.put("systemid", info.getCellIdentity().getSystemId());
        updateCellInfo("cellinfocdma", date, content);

        return toString(info);
    }

    private String storeCellInfoWcdma(Date date, CellInfoWcdma info) {
        ContentValues content = new ContentValues();
        content.put("registered", info.isRegistered() ? 1 : 0);
        content.put("radio", "UMTS");
        content.put("mcc", info.getCellIdentity().getMcc());
        content.put("mnc", info.getCellIdentity().getMnc());
        content.put("area", info.getCellIdentity().getLac());
        content.put("cid", info.getCellIdentity().getCid());
        content.put("psc", info.getCellIdentity().getPsc());
        content.put("uarfcn", info.getCellIdentity().getUarfcn());
        updateCellInfo("cellinfo", date, content);

        return toString(info);
    }

    private String storeCellInfoLte(Date date, CellInfoLte info) {
        ContentValues content = new ContentValues();
        content.put("registered", info.isRegistered() ? 1 : 0);
        content.put("radio", "LTE");
        content.put("mcc", info.getCellIdentity().getMcc());
        content.put("mnc", info.getCellIdentity().getMnc());
        content.put("area", info.getCellIdentity().getTac());
        content.put("cid", info.getCellIdentity().getCi());
        content.put("pci", info.getCellIdentity().getPci());
        updateCellInfo("cellinfo", date, content);

        return toString(info);
    }

    protected String getMetaEntry(String name) {
        Cursor c = db.query("meta", new String[]{"value"}, "entry = ?", new String[]{"versionCode"}, null, null, null);
        if (!c.moveToNext())
            return null;

        return c.getString(0);
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
        String versionCode = getMetaEntry("versionCode");
        if (versionCode == null)
            return -1;
        else
            return Integer.parseInt(versionCode);
    }

    public void storePhoneID(Context ctx) {
        String android_id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        setMetaEntry("android_id", android_id);
    }

    protected static void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
    }

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meta ("+
                "  entry VARCHAR(200) NOT NULL PRIMARY KEY,"+
                "  value TEXT NOT NULL"+
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS cellinfo ("+
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

        db.execSQL("CREATE TABLE IF NOT EXISTS cellinfocdma ("+
                "  date_start INT NOT NULL,"+
                "  date_end INT NOT NULL,"+
                "  registered INT NOT NULL,"+
                "  basestationid INT NOT NULL,"+
                "  latitude INT NOT NULL,"+
                "  longitude INT NOT NULL,"+
                "  networkid INT NOT NULL,"+
                "  systemid INT NOT NULL"+
                ")");
    }

    public void dropTables() {
        db.execSQL("DROP TABLE IF EXISTS meta");
        db.execSQL("DROP TABLE IF EXISTS cellinfo");
        db.execSQL("DROP TABLE IF EXISTS cellinfocdma");
    }
}
