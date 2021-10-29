package nl.nfi.cellscanner.collect.cellinfo;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@RequiresApi(api = Build.VERSION_CODES.S)
public class TelephonyCellInfoCollector extends SubscriptionDataCollector {
    public static final String[] PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
    };

    public TelephonyCellInfoCollector(Context ctx) {
        super(ctx);
    }

    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, TelephonyManager telephonyManager, String name) {
        return new CellInfoCallback(ctx, telephonyManager, name);
    }

    @Override
    public String[] requiredPermissions() {
        return PERMISSIONS;
    }

    public static class CellInfoCallback extends TelephonyCallback implements SubscriptionDataCollector.PhoneStateCallback, TelephonyCallback.CellInfoListener {
        private final Context ctx;
        private final TelephonyManager mgr;

        private final CellInfoState state;

        public CellInfoCallback(Context ctx, TelephonyManager telephonyManager, String name) {
            this.ctx = ctx;
            mgr = telephonyManager;
            state = new CellInfoState(name);
        }

        @Override
        public void start() {
            if (PermissionSupport.hasPermissions(ctx, PERMISSIONS)) {
                mgr.registerTelephonyCallback(ctx.getMainExecutor(), this);
            } else {
                Log.w("cellscanner", "insufficient permissions to get cell info");
                stop();
            }
        }

        @Override
        public void stop() {
            mgr.unregisterTelephonyCallback(this);
            state.updateCellStatus(null);
        }

        @Override
        public void onCellInfoChanged(@NonNull List<CellInfo> list) {
            state.updateCellInfo(list);
        }
    }
}
