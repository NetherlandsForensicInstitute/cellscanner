package nl.nfi.cellscanner.collect.telephonycallback;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@RequiresApi(api = Build.VERSION_CODES.S)
public class CallStateFactory implements SubscriptionDataCollector.CallbackFactory {
    @Override
    public boolean getRunState(Context ctx, Intent intent) {
        return Preferences.isRecordingEnabled(ctx, intent);
    }

    @Override
    public SubscriptionDataCollector.PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
        return new CellInfoCallback(subscription_id, name, defaultTelephonyManager, service);
    }

    public static class CellInfoCallback extends TelephonyCallback implements SubscriptionDataCollector.PhoneStateCallback, TelephonyCallback.CallStateListener {
        private final RecordingService service;
        private final TelephonyManager mgr;
        private final String subscription;

        public CellInfoCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, RecordingService service) {
            this.service = service;
            mgr = defaultTelephonyManager.createForSubscriptionId(subscription_id);
            subscription = name;
        }

        @Override
        public void resume() {
            Context ctx = service.getApplicationContext();
            if (PermissionSupport.hasCallStatePermission(ctx) && PermissionSupport.hasCourseLocationPermission(ctx) && PermissionSupport.hasFineLocationPermission(ctx)) {
                mgr.registerTelephonyCallback(service.getMainExecutor(), this);
            } else {
                Log.w("cellscanner", "insufficient permissions to get cell info");
                stop();
            }
        }

        @Override
        public void stop() {
            mgr.unregisterTelephonyCallback(this);
        }

        @Override
        public void onCallStateChanged(int i) {
            service.registerCallState(subscription, i);
        }
    }
}
