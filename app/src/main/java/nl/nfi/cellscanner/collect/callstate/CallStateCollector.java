package nl.nfi.cellscanner.collect.callstate;

import android.content.Context;
import android.content.Intent;

import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;

public class CallStateCollector implements DataCollector {
    private final DataReceiver receiver;
    private DataCollector collector = null;

    public CallStateCollector(Context ctx) {
        this.receiver = new DataReceiver(ctx);
    }

    private DataCollector createCollector(Intent intent) {
        return new PhoneStateCallStateCollector(receiver);
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
    public void resume(Intent intent) {
        updateCollector(intent);
        collector.resume(intent);
    }

    @Override
    public void cleanup() {
        updateCollector(null);
        collector.cleanup();
    }
}
