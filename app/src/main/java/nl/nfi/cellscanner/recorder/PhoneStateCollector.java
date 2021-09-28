package nl.nfi.cellscanner.recorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.legacyphonestate.PhoneStateFactory;

public class PhoneStateCollector {
    private final LocationRecordingService service;
    private SubscriptionManager subscriptionManager;
    private TelephonyManager defaultTelephonyManager;
    private Map<String, PhoneStateCallback> cellinfo_callbacks = new HashMap<>();
    private Map<String, PhoneStateCallback> callstate_callbacks = new HashMap<>();

    public interface CallbackFactory {
        PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service);
    }

    public interface PhoneStateCallback {
        void resume();

        void stop();
    }

    public PhoneStateCollector(LocationRecordingService service) {
        this.service = service;

        defaultTelephonyManager = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = (SubscriptionManager) service.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                service.updateRecordingState(service.getApplicationContext(), null);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private synchronized Map<String, PhoneStateCallback> updateCallbacks(Context ctx, Map<String, PhoneStateCallback> old_list, CallbackFactory factory, boolean run_state) {
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
            phst.resume();
        }

        return new_list;
    }

    private void update(Context ctx, boolean cellinfo_run_state, boolean callstate_run_state) {
        CallbackFactory cellinfo_factory = new PhoneStateFactory.CellInfoFactory();
        CallbackFactory callstate_factory = new PhoneStateFactory.CellInfoFactory();

        cellinfo_callbacks = updateCallbacks(ctx, cellinfo_callbacks, cellinfo_factory, cellinfo_run_state);
        callstate_callbacks = updateCallbacks(ctx, callstate_callbacks, callstate_factory, callstate_run_state);
    }

    public void update(Context ctx, Intent intent) {
        boolean cellinfo_run_state = Preferences.isRecordingEnabled(ctx, intent);
        boolean callstate_run_state = Preferences.isCallStateRecordingEnabled(ctx, intent) && PermissionSupport.hasCallStatePermission(ctx);
        update(ctx, cellinfo_run_state, callstate_run_state);
    }

    public void cleanup(Context ctx) {
        update(ctx, false, false);
    }
}
