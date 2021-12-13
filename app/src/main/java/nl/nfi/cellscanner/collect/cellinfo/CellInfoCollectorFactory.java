package nl.nfi.cellscanner.collect.cellinfo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;

public class CellInfoCollectorFactory extends CollectorFactory {
    @Override
    public String getTitle() {
        return "cell info";
    }

    @Override
    public String getStatusText() {
        return CellscannerApp.getDatabase().getUpdateStatus();
    }

    @Override
    public DataCollector createCollector(Context ctx) {
        return new CellInfoCollector(ctx);
    }

    @Override
    public void createTables(SQLiteDatabase db) {
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
                "  arfcn INT,"+ // Absolute RF Channel Number (GSM, LTE, NR)
                "  psc INT,"+ // 9-bit UMTS Primary Scrambling Code described in TS 25.331 (UMTS only/)
                "  uarfcn INT,"+ // 16-bit UMTS Absolute RF Channel Number (UMTS only)
                "  pci INT,"+ // Physical Cell Id (LTE: 0..503 or Integer.MAX_VALUE if unknown; NR: 0..1007 or CellInfo.UNAVAILABLE)
                "  bandwidth INT"+ // bandwidth in KHz (LTE only)
                ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS cellinfo_date_end ON cellinfo(date_end)");
    }

    @Override
    public void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // no upgrade for versions prior to 2
            return;
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE cellinfo ADD COLUMN subscription VARCHAR(20) NOT NULL DEFAULT 'unknown'");
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE cellinfo ADD COLUMN bandwidth INT");
        }
    }

    @Override
    public void dropDataUntil(SQLiteDatabase db, long timestamp) {
        db.delete("cellinfo", "date_end <= ?", new String[]{Long.toString(timestamp)});
    }

    /**
     * Update the current cellular connection status.
     *
     * @param status the cellular connection status
     */
    protected static void updateCellStatus(SQLiteDatabase db, String subscription, Date date_start, Date date_end, CellStatus status) {
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

}
