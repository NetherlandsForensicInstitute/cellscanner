package nl.nfi.cellscanner.collect.telephonycallback;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@RequiresApi(api = Build.VERSION_CODES.S)
public class TelephonyCallStateCollector extends SubscriptionDataCollector {
    public static final String[] PERMISSIONS = new String[] {
            Manifest.permission.READ_PHONE_STATE,
    };

    public TelephonyCallStateCollector(DataReceiver receiver) {
        super(receiver);
    }

    @Override
    public String[] requiredPermissions() {
        return PERMISSIONS;
    }

    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        return new CellInfoCallback(ctx, subscription_id, name, defaultTelephonyManager, service);
    }

    public static class CellInfoCallback extends TelephonyCallback implements SubscriptionDataCollector.PhoneStateCallback, TelephonyCallback.CallStateListener {
        private final Context ctx;
        private final DataReceiver service;
        private final TelephonyManager mgr;
        private final String subscription;

        public CellInfoCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
            this.ctx = ctx;
            this.service = service;
            mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
            subscription = name;
        }

        @Override
        public void resume() {
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
            service.storeCallState(subscription, i);
        }
    }
}
