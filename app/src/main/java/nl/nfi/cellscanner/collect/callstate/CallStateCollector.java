package nl.nfi.cellscanner.collect.callstate;

import android.content.Context;
import android.content.Intent;

import nl.nfi.cellscanner.collect.DataCollector;

public class CallStateCollector implements DataCollector {
    private final Context ctx;
    private DataCollector collector = null;

    public CallStateCollector(Context ctx) {
        this.ctx = ctx;
    }

    private DataCollector createCollector(Intent intent) {
        return new PhoneStateCallStateCollector(ctx);
    }

    private void updateCollector(Intent intent) {
        if (collector == null)
            collector = createCollector(intent);
    }

    @Override
    public String[] requiredPermissions(Intent intent) {
        updateCollector(intent);
        return collector.requiredPermissions(intent);
    }

    @Override
    public void start(Intent intent) {
        updateCollector(intent);
        collector.start(intent);
    }

    @Override
    public void stop() {
        updateCollector(null);
        collector.stop();
    }
}
