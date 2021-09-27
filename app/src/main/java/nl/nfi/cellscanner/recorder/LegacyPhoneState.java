package nl.nfi.cellscanner.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import nl.nfi.cellscanner.CellScannerApp;
import nl.nfi.cellscanner.CellStatus;
import nl.nfi.cellscanner.Preferences;

public class LegacyPhoneState {
    private final LocationRecordingService service;
    private SubscriptionManager subscriptionManager;
    private TelephonyManager defaultTelephonyManager;
    private Map<String, PhoneStateCallback> telephonyManagers = new HashMap<>();

    private class PhoneStateCallback extends PhoneStateListener {
        private final String subscription;
        private final TelephonyManager mgr;

        private CellStatus cell_status = null;
        private Date cell_start_timestamp = null;

        public PhoneStateCallback(int subscription_id, String name) {
            subscription = name;
            mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
        }

        @SuppressLint("MissingPermission")
        public void listen(int events) {
            flush(new Date());
            Log.e("cellscanner", "listen"+subscription+" to "+events);
            mgr.listen(this, events);
            if ((events & PhoneStateListener.LISTEN_CELL_INFO) != 0) {
                Log.e("cellscanner", "refresh"+subscription);
                onCellInfoChanged(mgr.getAllCellInfo());
            }
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            try {
                service.registerCallState(subscription, state);
            } catch (Throwable e) {
                CellScannerApp.getDatabase().storeMessage(e);
            }
        }

        private void flush(Date end_date) {
            if (cell_status != null)
                store(end_date);
            cell_status = null;
            cell_start_timestamp = null;
        }

        private void store(Date end_date) {
            if (cell_status != null)
                service.registerCellStatus(subscription, cell_start_timestamp, end_date, cell_status);
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            Log.e("cellscanner", "results for"+subscription);
            try {
                CellStatus next_status = null;
                for (CellInfo info : cellInfo) {
                    CellStatus status = CellStatus.fromCellInfo(info);
                    if (status.isValid()) {
                        next_status = status;
                        Log.e("cellscanner", "status for "+subscription+" is "+next_status);
                    }
                }

                if (next_status == null) {
                    // update database entry with new end date and clear current status
                    flush(new Date());
                } else if (next_status.equals(cell_status)) {
                    // no change, update database entry with new end date
                    store(new Date());
                } else {
                    // flush old and create new entry
                    Date ts = new Date();
                    flush(ts);
                    cell_status = next_status;
                    cell_start_timestamp = ts;
                    store(ts);
                }
            } catch (Throwable e) {
                CellScannerApp.getDatabase().storeMessage(e);
            }
        }
    }

    public LegacyPhoneState(LocationRecordingService service) {
        this.service = service;

        defaultTelephonyManager = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = (SubscriptionManager) service.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        subscriptionManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener(){
            @Override
            public void onSubscriptionsChanged() {
                service.updateRecordingState(service.getApplicationContext(), null);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private synchronized void registerPhoneStateCallback(Context ctx, int events) {

        Map<String, PhoneStateCallback> old_managers = telephonyManagers;
        Map<String, PhoneStateCallback> new_managers = new HashMap<>();
        if (PermissionSupport.hasCallStatePermission(ctx)) {
            for (SubscriptionInfo subscr : subscriptionManager.getActiveSubscriptionInfoList()) {
                String subscription_name = subscr.getDisplayName().toString();
                if (old_managers.containsKey(subscription_name)) {
                    new_managers.put(subscription_name, old_managers.get(subscription_name));
                    old_managers.remove(subscription_name);
                } else {
                    new_managers.put(subscription_name, new PhoneStateCallback(subscr.getSubscriptionId(), subscription_name));
                }
            }
        }

        // unregister listeners for removed subscriptions
        for (PhoneStateCallback phst : old_managers.values()) {
            phst.listen(PhoneStateListener.LISTEN_NONE);
        }

        for (PhoneStateCallback phst : new_managers.values()) {
            phst.listen(events);
        }

        telephonyManagers = new_managers;
    }

    public void update(Context ctx, Intent intent) {
        int events = PhoneStateListener.LISTEN_NONE;
        if (Preferences.isRecordingEnabled(ctx, intent) && PermissionSupport.hasCallStatePermission(ctx) && PermissionSupport.hasCourseLocationPermission(ctx) && PermissionSupport.hasFineLocationPermission(ctx))
            events |= PhoneStateListener.LISTEN_CELL_INFO;
        if (Preferences.isCallStateRecordingEnabled(ctx, intent) && PermissionSupport.hasCallStatePermission(ctx))
            events |= PhoneStateListener.LISTEN_CALL_STATE;

        registerPhoneStateCallback(ctx, events);
    }

    public void cleanup(Context ctx) {
        registerPhoneStateCallback(ctx, PhoneStateListener.LISTEN_NONE);
    }
}
