package nl.nfi.cellscanner.legacyphonestate;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.nfi.cellscanner.CellScannerApp;
import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.PermissionSupport;

public class CallStateCallback extends AbstractCallback {
    public CallStateCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service) {
        super(subscription_id, name, defaultTelephonyManager, service);
    }

    @Override
    public void resume() {
        Context ctx = service.getApplicationContext();
        if (PermissionSupport.hasCallStatePermission(ctx)) {
            super.listen(PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            Log.w("cellscanner", "insufficient permissions to get cell info");
            stop();
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        try {
            service.registerCallState(subscription, state);
        } catch (Throwable e) {
            CellScannerApp.getDatabase().storeMessage(e);
        }
    }
}
