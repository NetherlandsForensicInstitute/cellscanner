package nl.nfi.cellscanner.collect;

import android.content.Context;
import android.location.Location;

import java.util.Date;

import nl.nfi.cellscanner.collect.cellinfo.CellStatus;
import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.ViewMeasurementsActivity;

public class DataReceiver {
    private final Context ctx;

    public DataReceiver(Context ctx) {
        this.ctx = ctx;
    }

    public Context getContext() {
        return ctx;
    }

    public void storeCellStatus(String subscription, Date date_start, Date date_end, CellStatus status) {
        CellscannerApp.getDatabase().updateCellStatus(subscription, date_start, date_end, status);
        ViewMeasurementsActivity.refresh(ctx);
    }

    public void storeCallState(String subscription, int state) {
        CellscannerApp.getDatabase().storeCallState(state);
        ViewMeasurementsActivity.refresh(ctx);
    }

    public void storeLocation(Location location) {
        CellscannerApp.getDatabase().storeLocationInfo(location);
        ViewMeasurementsActivity.refresh(ctx);
    }
}
