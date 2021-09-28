package nl.nfi.cellscanner.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.Map;

import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.PermissionSupport;

public abstract class SubscriptionDataCollector implements LocationRecordingService.DataCollector {
    private final LocationRecordingService service;
    private SubscriptionManager subscriptionManager;
    private TelephonyManager defaultTelephonyManager;

    public interface CallbackFactory {
        boolean getRunState(Context ctx, Intent intent);
        PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service);
    }

    public interface PhoneStateCallback {
        void resume();
        void stop();
    }

    public SubscriptionDataCollector(LocationRecordingService service) {
        this.service = service;

        defaultTelephonyManager = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = (SubscriptionManager) service.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                resume(service.getApplicationContext(), null);
            }
        });
    }

    @SuppressLint("MissingPermission")
    protected synchronized Map<String, PhoneStateCallback> updateCallbacks(Context ctx, Intent intent, Map<String, PhoneStateCallback> old_list, CallbackFactory factory, boolean enable) {
        Map<String, PhoneStateCallback> new_list = new HashMap<>();
        if (PermissionSupport.hasCallStatePermission(ctx)) {
            for (SubscriptionInfo subscr : subscriptionManager.getActiveSubscriptionInfoList()) {
                String subscription_name = subscr.getDisplayName().toString();
                if (old_list.containsKey(subscription_name)) {
                    new_list.put(subscription_name, old_list.get(subscription_name));
                    old_list.remove(subscription_name);
                } else {
                    new_list.put(subscription_name, factory.createCallback(subscr.getSubscriptionId(), subscription_name, defaultTelephonyManager, service));
                }
            }
        }

        // unregister listeners for removed subscriptions
        for (PhoneStateCallback phst : old_list.values()) {
            phst.stop();
        }

        // unregister listeners for removed subscriptions
        for (PhoneStateCallback phst : new_list.values()) {
            if (enable && factory.getRunState(ctx, intent))
                phst.resume();
            else
                phst.stop();
        }

        return new_list;
    }

    public abstract void update(Context ctx, Intent intent, boolean enable);

    @Override
    public void resume(Context ctx, Intent intent) {
        update(ctx, intent, true);
    }

    @Override
    public void cleanup(Context ctx) {
        update(ctx, null, false);
    }
}
