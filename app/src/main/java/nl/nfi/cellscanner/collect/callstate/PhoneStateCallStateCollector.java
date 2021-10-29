package nl.nfi.cellscanner.collect.callstate;

import android.Manifest;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;
import nl.nfi.cellscanner.collect.AbstractCallback;

/**
 * Uses API which is deprecated at API level 31
 */
@Deprecated
public class PhoneStateCallStateCollector extends SubscriptionDataCollector {
    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
    };

    public PhoneStateCallStateCollector(Context ctx) {
        super(ctx);
    }

    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, TelephonyManager telephonyManager, String name) {
        return new CallStateCallback(ctx, telephonyManager, name);
    }

    @Override
    public String[] requiredPermissions() {
        return PERMISSIONS;
    }

    public static class CallStateCallback extends AbstractCallback {
        private final Context ctx;

        public CallStateCallback(Context ctx, TelephonyManager telephonyManager, String name) {
            super(telephonyManager, name);
            this.ctx = ctx;
        }

        @Override
        public void start() {
            if (PermissionSupport.hasPermissions(ctx, PERMISSIONS)) {
                super.listen(PhoneStateListener.LISTEN_CALL_STATE);
            } else {
                Log.w("cellscanner", "insufficient permissions to get call state");
                stop();
            }
        }

        @Override
        public void stop() {
            super.listen(PhoneStateListener.LISTEN_NONE);
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            try {
                CallStateCollectorFactory.storeCallState(subscription, state);
            } catch (Throwable e) {
                CellscannerApp.getDatabase().storeMessage(e);
            }
        }
    }
}
