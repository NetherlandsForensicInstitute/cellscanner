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
    private final Context ctx;
    private SubscriptionManager subscriptionManager = null;
    private final TelephonyManager defaultTelephonyManager;

    private Map<String, PhoneStateCallback> callbacks = new HashMap<>();

    public abstract String[] requiredPermissions();

    public String[] requiredPermissions(Intent intent) {
        return requiredPermissions();
    }
    public abstract PhoneStateCallback createCallback(Context ctx, TelephonyManager telephonyManager, String name);

    public interface PhoneStateCallback {
        void start();
        void stop();
    }

    public SubscriptionDataCollector(Context ctx) {
        this.ctx = ctx;
        defaultTelephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @SuppressLint("MissingPermission")
    protected synchronized Map<String, PhoneStateCallback> updateCallbacks(Intent intent, Map<String, PhoneStateCallback> old_list, boolean enable) {
        Map<String, PhoneStateCallback> new_list = new HashMap<>();
        if (PermissionSupport.hasPermissions(ctx, requiredPermissions())) {
            for (SubscriptionInfo subscr : subscriptionManager.getActiveSubscriptionInfoList()) {
                String subscription_name = subscr.getDisplayName().toString();
                if (old_list.containsKey(subscription_name)) {
                    new_list.put(subscription_name, old_list.get(subscription_name));
                    old_list.remove(subscription_name);
                } else {
                    TelephonyManager mgr = defaultTelephonyManager.createForSubscriptionId(subscr.getSubscriptionId());
                    new_list.put(subscription_name, createCallback(ctx, mgr, subscription_name));
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
                phst.start();
            else
                phst.stop();
        }

        return new_list;
    }

    public void update(Intent intent, boolean enable) {
        callbacks = updateCallbacks(intent, callbacks, enable);
    }

    @Override
    public void start(Intent intent) {
        if (subscriptionManager == null) {
            subscriptionManager = (SubscriptionManager) ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                start(null);
                }
            });
        } else {
            update(intent, true);
        }
    }

    @Override
    public void stop() {
        update(null, false);
    }
}
