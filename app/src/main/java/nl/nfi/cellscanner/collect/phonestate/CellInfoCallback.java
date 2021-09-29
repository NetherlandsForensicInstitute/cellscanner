package nl.nfi.cellscanner.collect.phonestate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import nl.nfi.cellscanner.collect.CellInfoState;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.PermissionSupport;

public class CellInfoCallback extends AbstractCallback {
    public static final String[] PERMISSIONS = new String[] {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
    };

    private final Context ctx;
    private final CellInfoState state;

    public CellInfoCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        super(subscription_id, name, defaultTelephonyManager, service);
        this.ctx = ctx;
        state = new CellInfoState(name, service);
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        if (PermissionSupport.hasPermissions(ctx, PERMISSIONS)) {
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
