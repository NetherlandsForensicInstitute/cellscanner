package nl.nfi.cellscanner.collect.phonestate;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

public abstract class AbstractCallback extends PhoneStateListener implements SubscriptionDataCollector.PhoneStateCallback {
    protected final String subscription;
    protected final TelephonyManager mgr;

    public AbstractCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        subscription = name;
        mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
    }

    protected void listen(int events) {
        mgr.listen(this, events);
    }
}
