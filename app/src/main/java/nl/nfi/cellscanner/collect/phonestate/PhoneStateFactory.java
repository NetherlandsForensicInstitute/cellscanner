package nl.nfi.cellscanner.collect.phonestate;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

public class PhoneStateFactory {
    public static class CellInfoFactory implements SubscriptionDataCollector.CallbackFactory {
        @Override
        public boolean getRunState(Context ctx, Intent intent) {
            return Preferences.isRecordingEnabled(ctx, intent);
        }

        @Override
        public SubscriptionDataCollector.PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
            return new CellInfoCallback(subscription_id, name, defaultTelephonyManager, service);
        }
    }

    public static class CallStateFactory implements SubscriptionDataCollector.CallbackFactory {
        @Override
        public boolean getRunState(Context ctx, Intent intent) {
            return Preferences.isCallStateRecordingEnabled(ctx, intent);
        }

        @Override
        public SubscriptionDataCollector.PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
            return new CallStateCallback(subscription_id, name, defaultTelephonyManager, service);
        }
    }
}
