package nl.nfi.cellscanner.legacyphonestate;

import android.telephony.TelephonyManager;

import nl.nfi.cellscanner.recorder.LocationRecordingService;
import nl.nfi.cellscanner.recorder.PhoneStateCollector;

public class PhoneStateFactory {
    public static class CellInfoFactory implements PhoneStateCollector.CallbackFactory {
        public PhoneStateCollector.PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service) {
            return new CellInfoCallback(subscription_id, name, defaultTelephonyManager, service);
        }
    }

    public static class CallStateFactory implements PhoneStateCollector.CallbackFactory {
        public PhoneStateCollector.PhoneStateCallback createCallback(int subscription_id, String name, TelephonyManager defaultTelephonyManager, LocationRecordingService service) {
            return new CallStateCallback(subscription_id, name, defaultTelephonyManager, service);
        }
    }
}
