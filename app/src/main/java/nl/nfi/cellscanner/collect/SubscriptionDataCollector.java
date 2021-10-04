package nl.nfi.cellscanner.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.Map;

import nl.nfi.cellscanner.PermissionSupport;

public abstract class SubscriptionDataCollector implements DataCollector {
    private final DataReceiver receiver;
    private SubscriptionManager subscriptionManager = null;
    private final TelephonyManager defaultTelephonyManager;

    private Map<String, PhoneStateCallback> callbacks = new HashMap<>();

    public abstract String[] requiredPermissions();

    public String[] requiredPermissions(Intent intent) {
        return requiredPermissions();
    }
    public abstract PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service);

    public interface PhoneStateCallback {
        void resume();
        void stop();
    }

    public SubscriptionDataCollector(DataReceiver receiver) {
        this.receiver = receiver;
        defaultTelephonyManager = (TelephonyManager) receiver.getContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @SuppressLint("MissingPermission")
    protected synchronized Map<String, PhoneStateCallback> updateCallbacks(Intent intent, Map<String, PhoneStateCallback> old_list, boolean enable) {
        Map<String, PhoneStateCallback> new_list = new HashMap<>();
        if (PermissionSupport.hasPermissions(receiver.getContext(), requiredPermissions())) {
            for (SubscriptionInfo subscr : subscriptionManager.getActiveSubscriptionInfoList()) {
                String subscription_name = subscr.getDisplayName().toString();
                if (old_list.containsKey(subscription_name)) {
                    new_list.put(subscription_name, old_list.get(subscription_name));
                    old_list.remove(subscription_name);
                } else {
                    new_list.put(subscription_name, createCallback(receiver.getContext(), subscr.getSubscriptionId(), subscription_name, defaultTelephonyManager, receiver));
                }
            }
        }

        // unregister listeners for removed subscriptions
        for (PhoneStateCallback phst : old_list.values()) {
            phst.stop();
        }

        // unregister listeners for removed subscriptions
        for (PhoneStateCallback phst : new_list.values()) {
            if (enable)
                phst.resume();
            else
                phst.stop();
        }

        return new_list;
    }

    public void update(Intent intent, boolean enable) {
        callbacks = updateCallbacks(intent, callbacks, enable);
    }

    @Override
    public void resume(Intent intent) {
        if (subscriptionManager == null) {
            subscriptionManager = (SubscriptionManager) receiver.getContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                resume(null);
                }
            });
        } else {
            update(intent, true);
        }
    }

    @Override
    public void cleanup() {
        update(null, false);
    }
}
