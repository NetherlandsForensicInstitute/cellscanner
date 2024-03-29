package nl.nfi.cellscanner.collect.cellinfo;

import android.database.sqlite.SQLiteDatabase;
import android.telephony.CellInfo;

import java.util.Date;
import java.util.List;

import nl.nfi.cellscanner.CellscannerApp;

public class CellInfoState {
    private final String subscription;

    private CellStatus cell_status = null;
    private Date cell_start_timestamp = null;

    public CellInfoState(String substription) {
        this.subscription = substription;
    }

    public void updateCellInfo(List<CellInfo> cellInfo) {
        if (cellInfo == null) {
            updateCellStatus(null);
            return;
        }

        try {
            for (CellInfo info : cellInfo) {
                CellStatus status = CellStatus.fromCellInfo(info);
                if (status.isValid())
                    updateCellStatus(status);
            }
        } catch (CellStatus.UnsupportedTypeException e) {
            CellscannerApp.getDatabase().storeMessage(e);
        }
    }

    public void updateCellStatus(CellStatus next_status) {
        if (next_status == null) {
            // update database entry with new end date and clear current status
            flush(new Date());
        } else if (next_status.equals(cell_status)) {
            // no change, update database entry with new end date
            store(new Date());
        } else {
            // flush old and create new entry
            Date ts = new Date();
            flush(ts);
            cell_status = next_status;
            cell_start_timestamp = ts;
            store(ts);
        }
    }

    private void flush(Date end_date) {
        if (cell_status != null)
            store(end_date);
        cell_status = null;
        cell_start_timestamp = null;
    }

    private void store(Date end_date) {
        if (cell_status != null) {
            SQLiteDatabase db = CellscannerApp.getDatabaseConnection();
            try {
                CellInfoCollectorFactory.updateCellStatus(db, subscription, cell_start_timestamp, end_date, cell_status);
            } finally {
                db.close();
            }
        }
    }
}
