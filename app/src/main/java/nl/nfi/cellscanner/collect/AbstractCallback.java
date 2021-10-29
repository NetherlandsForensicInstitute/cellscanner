package nl.nfi.cellscanner.collect;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public abstract class AbstractCallback extends PhoneStateListener implements SubscriptionDataCollector.PhoneStateCallback {
    protected final String subscription;
    protected final TelephonyManager mgr;
    private int listen_events = PhoneStateListener.LISTEN_NONE;

    public AbstractCallback(TelephonyManager telephonyManager, String name) {
        subscription = name;
        mgr = telephonyManager;
    }

    protected void listen(int events) {
        if (events != listen_events) {
            mgr.listen(this, events);
            listen_events = events;
        }
    }
}
