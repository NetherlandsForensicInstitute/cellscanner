package nl.nfi.cellscanner.collect.phonestate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import nl.nfi.cellscanner.collect.CellInfoState;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.PermissionSupport;

public class CellInfoCallback extends AbstractCallback {
    private final CellInfoState state;

    public CellInfoCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
        super(subscription_id, name, defaultTelephonyManager, service);
        state = new CellInfoState(name, service);
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
        super.listen(PhoneStateListener.LISTEN_NONE);
        state.onCellInfoChanged(null);
    }

    @Override
    public void onCellInfoChanged(List<CellInfo> cellInfo) {
        state.onCellInfoChanged(cellInfo);
    }

}
