package nl.nfi.cellscanner.collect.callstate;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.cellinfo.CellInfoCollector;

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
}
