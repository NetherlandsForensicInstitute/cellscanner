package nl.nfi.cellscanner.collect.callstate;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;

import java.util.Date;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;

public class CallStateCollectorFactory extends CollectorFactory {
    @Override
    public String getTitle() {
        return "call state";
    }

    @Override
    public String getStatusText() {
        return "";
    }

    @Override
    public DataCollector createCollector(Context ctx) {
        return new CallStateCollector(ctx);
    }

    @Override
    public void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS call_state ("+
                "  date INT NOT NULL,"+
                "  state VARCHAR(20) NOT NULL"+
                ")");
    }

    @Override
    public void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void dropDataUntil(SQLiteDatabase db, long timestamp) {
    }

    protected static void storeCallState(String subscription, int state) {
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

        SQLiteDatabase db = CellscannerApp.getDatabaseConnection();
        try {
            db.insert("call_state", null, values);
        } finally {
            db.close();
        }
    }
}
