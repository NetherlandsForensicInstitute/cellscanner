package nl.nfi.cellscanner.collect.phonestate;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

public abstract class AbstractCallback extends PhoneStateListener implements SubscriptionDataCollector.PhoneStateCallback {
    protected final String subscription;
    protected final TelephonyManager mgr;
    protected final RecordingService service;

    public AbstractCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
        subscription = name;
        mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
        this.service = service;
    }

    protected void listen(int events) {
        mgr.listen(this, events);
    }
}
