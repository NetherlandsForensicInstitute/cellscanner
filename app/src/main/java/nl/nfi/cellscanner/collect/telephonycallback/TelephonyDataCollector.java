package nl.nfi.cellscanner.collect.telephonycallback;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Map;

import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;

@RequiresApi(api = Build.VERSION_CODES.S)
public class TelephonyDataCollector extends SubscriptionDataCollector {
    private Map<String, SubscriptionDataCollector.PhoneStateCallback> cellinfo_callbacks = new HashMap<>();
    private Map<String, SubscriptionDataCollector.PhoneStateCallback> callstate_callbacks = new HashMap<>();

    public TelephonyDataCollector(RecordingService service) {
        super(service);
    }

    public void update(Context ctx, Intent intent, boolean enable) {
        SubscriptionDataCollector.CallbackFactory cellinfo_factory = new CellInfoFactory();
        SubscriptionDataCollector.CallbackFactory callstate_factory = new CallStateFactory();

        cellinfo_callbacks = updateCallbacks(ctx, intent, cellinfo_callbacks, cellinfo_factory, enable);
        callstate_callbacks = updateCallbacks(ctx, intent, callstate_callbacks, callstate_factory, enable);
    }
}
