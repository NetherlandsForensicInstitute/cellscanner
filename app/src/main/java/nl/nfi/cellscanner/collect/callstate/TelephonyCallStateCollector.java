package nl.nfi.cellscanner.collect.callstate;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@RequiresApi(api = Build.VERSION_CODES.S)
public class TelephonyCallStateCollector extends SubscriptionDataCollector {
    public static final String[] PERMISSIONS = new String[] {
            Manifest.permission.READ_PHONE_STATE,
    };

    public TelephonyCallStateCollector(Context ctx) {
        super(ctx);
    }

    @Override
    public String[] requiredPermissions() {
        return PERMISSIONS;
    }

    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, TelephonyManager telephonyManager, String name) {
        return new CellInfoCallback(ctx, telephonyManager, name);
    }

    public static class CellInfoCallback extends TelephonyCallback implements SubscriptionDataCollector.PhoneStateCallback, TelephonyCallback.CallStateListener {
        private final Context ctx;
        private final TelephonyManager mgr;
        private final String subscription;

        public CellInfoCallback(Context ctx, TelephonyManager telephonyManager, String name) {
            this.ctx = ctx;
            mgr = telephonyManager;
            subscription = name;
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
        }

        @Override
        public void onCallStateChanged(int i) {
            CallStateCollectorFactory.storeCallState(subscription, i);
        }
    }
}
