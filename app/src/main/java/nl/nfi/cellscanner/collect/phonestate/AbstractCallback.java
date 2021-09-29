package nl.nfi.cellscanner.collect.phonestate;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

public abstract class AbstractCallback extends PhoneStateListener implements SubscriptionDataCollector.PhoneStateCallback {
    protected final String subscription;
    protected final TelephonyManager mgr;
    private int listen_events = PhoneStateListener.LISTEN_NONE;

    public AbstractCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        subscription = name;
        mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
    }

    protected void listen(int events) {
        if (events != listen_events) {
            mgr.listen(this, events);
            listen_events = events;
        }
    }
}
