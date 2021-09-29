package nl.nfi.cellscanner.collect.phonestate;

import android.Manifest;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.PermissionSupport;

public class CallStateCallback extends AbstractCallback {
    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
    };

    private final Context ctx;
    private final DataReceiver receiver;

    public CallStateCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        super(subscription_id, name, defaultTelephonyManager, service);
        this.ctx = ctx;
        this.receiver = service;
    }

    @Override
    public void resume() {
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
            receiver.storeCallState(subscription, state);
        } catch (Throwable e) {
            CellscannerApp.getDatabase().storeMessage(e);
        }
    }
}
