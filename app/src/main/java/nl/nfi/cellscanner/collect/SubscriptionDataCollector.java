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

public class SubscriptionDataCollector implements DataCollector {
    private final DataReceiver receiver;
    private final SubscriptionCallbackFactory factory;
    private SubscriptionManager subscriptionManager = null;
    private final TelephonyManager defaultTelephonyManager;

    private Map<String, PhoneStateCallback> callbacks = new HashMap<>();

    public interface SubscriptionCallbackFactory {
        String[] requiredPermissions();
        PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service);
    }

    public interface PhoneStateCallback {
        void resume();
        void stop();
    }

    public SubscriptionDataCollector(DataReceiver receiver, SubscriptionCallbackFactory factory) {
        this.receiver = receiver;
        this.factory = factory;

        defaultTelephonyManager = (TelephonyManager) receiver.getContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @SuppressLint("MissingPermission")
    protected synchronized Map<String, PhoneStateCallback> updateCallbacks(Context ctx, Intent intent, Map<String, PhoneStateCallback> old_list, SubscriptionCallbackFactory factory, boolean enable) {
        Map<String, PhoneStateCallback> new_list = new HashMap<>();
        if (PermissionSupport.hasPermissions(ctx, requiredPermissions())) {
            for (SubscriptionInfo subscr : subscriptionManager.getActiveSubscriptionInfoList()) {
                String subscription_name = subscr.getDisplayName().toString();
                if (old_list.containsKey(subscription_name)) {
                    new_list.put(subscription_name, old_list.get(subscription_name));
                    old_list.remove(subscription_name);
                } else {
                    new_list.put(subscription_name, factory.createCallback(ctx, subscr.getSubscriptionId(), subscription_name, defaultTelephonyManager, receiver));
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

    public void update(Context ctx, Intent intent, boolean enable) {
        callbacks = updateCallbacks(ctx, intent, callbacks, factory, enable);
    }

    @Override
    public String[] requiredPermissions() {
        return factory.requiredPermissions();
    }

    @Override
    public void resume(Context ctx, Intent intent) {
        if (subscriptionManager == null) {
            subscriptionManager = (SubscriptionManager) receiver.getContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                resume(receiver.getContext(), null);
                }
            });
        } else {
            update(ctx, intent, true);
        }
    }

    @Override
    public void cleanup(Context ctx) {
        update(ctx, null, false);
    }
}
