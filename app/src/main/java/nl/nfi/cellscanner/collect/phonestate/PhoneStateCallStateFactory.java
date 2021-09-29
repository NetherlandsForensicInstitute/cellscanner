package nl.nfi.cellscanner.collect.phonestate;

import android.content.Context;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

/**
 * Uses API which is deprecated at API level 31
 */
@Deprecated
public class PhoneStateCallStateFactory implements SubscriptionDataCollector.SubscriptionCallbackFactory {
    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
        return new CallStateCallback(ctx, subscription_id, name, defaultTelephonyManager, service);
    }

    @Override
    public String[] requiredPermissions() {
        return CallStateCallback.PERMISSIONS;
    }
}
