package nl.nfi.cellscanner.collect.phonestate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@Deprecated
public class PhoneStateFactory {
    @Deprecated
    public static class CellInfoFactory implements SubscriptionDataCollector.SubscriptionCallbackFactory {
        @Override
        public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
            return new CellInfoCallback(ctx, subscription_id, name, defaultTelephonyManager, service);
        }

        @Override
        public String[] requiredPermissions() {
            return new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
            };
        }
    }

    @Deprecated
    public static class CallStateFactory implements SubscriptionDataCollector.SubscriptionCallbackFactory {
        @Override
        public SubscriptionDataCollector.PhoneStateCallback createCallback(Context ctx, int subscription_id, String name, TelephonyManager defaultTelephonyManager, DataReceiver service) {
            return new CallStateCallback(ctx, subscription_id, name, defaultTelephonyManager, service);
        }

        @Override
        public String[] requiredPermissions() {
            return new String[] {
                    Manifest.permission.READ_PHONE_STATE,
            };
        }
    }
}
