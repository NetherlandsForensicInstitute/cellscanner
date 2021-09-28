package nl.nfi.cellscanner.legacyphonestate;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.CellScannerApp;
import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.PhoneStateCollector;

public abstract class AbstractCallback extends PhoneStateListener implements PhoneStateCollector.PhoneStateCallback {
    protected final String subscription;
    protected final TelephonyManager mgr;
    protected final LocationRecordingService service;

    public AbstractCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service) {
        subscription = name;
        mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
        this.service = service;
    }

    protected void listen(int events) {
        mgr.listen(this, events);
    }
}
