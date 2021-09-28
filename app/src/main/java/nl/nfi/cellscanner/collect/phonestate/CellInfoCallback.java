package nl.nfi.cellscanner.collect.phonestate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.util.List;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.CellStatus;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.PermissionSupport;

public class CellInfoCallback extends AbstractCallback {
    private CellStatus cell_status = null;
    private Date cell_start_timestamp = null;

    public CellInfoCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
        super(subscription_id, name, defaultTelephonyManager, service);
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        Context ctx = service.getApplicationContext();
        if (PermissionSupport.hasCallStatePermission(ctx) && PermissionSupport.hasCourseLocationPermission(ctx) && PermissionSupport.hasFineLocationPermission(ctx)) {
            super.listen(PhoneStateListener.LISTEN_CELL_INFO);
            onCellInfoChanged(mgr.getAllCellInfo());
        } else {
            Log.w("cellscanner", "insufficient permissions to get cell info");
            stop();
        }
    }

    public void stop() {
        flush(new Date());
    }

    @Override
    public void onCellInfoChanged(List<CellInfo> cellInfo) {
        Log.e("cellscanner", "results for"+subscription);
        try {
            CellStatus next_status = null;
            for (CellInfo info : cellInfo) {
                CellStatus status = CellStatus.fromCellInfo(info);
                if (status.isValid()) {
                    next_status = status;
                    Log.e("cellscanner", "status for "+subscription+" is "+next_status);
                }
            }

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
        } catch (Throwable e) {
            CellscannerApp.getDatabase().storeMessage(e);
        }
    }

    private void flush(Date end_date) {
        if (cell_status != null)
            store(end_date);
        cell_status = null;
        cell_start_timestamp = null;
    }

    private void store(Date end_date) {
        if (cell_status != null)
            service.registerCellStatus(subscription, cell_start_timestamp, end_date, cell_status);
    }
}
