package nl.nfi.cellscanner.collect.cellinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.phonestate.PhoneStateCellInfoCollector;
import nl.nfi.cellscanner.collect.telephonycallback.TelephonyCellInfoCollector;

public class CellInfoCollector implements DataCollector {
    private final DataReceiver receiver;
    private DataCollector collector = null;

    public CellInfoCollector(DataReceiver receiver) {
        this.receiver = receiver;
    }

    private DataCollector createCollector(Intent intent) {
        String method = Preferences.getCellInfoMethod(receiver.getContext(), intent);

        if (method.equals("AUTO")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                method = "TELEPHONY_CALLBACK";
            } else {
                method = "PHONE_STATE";
            }
        }

        if (method.equals("TELEPHONY_CALLBACK") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return new TelephonyCellInfoCollector(receiver);
        else if (method.equals("PHONE_STATE"))
            return new PhoneStateCellInfoCollector(receiver);
        else if (method.equals("BASIC"))
            return new TimerCellInfoCollector(receiver);
        else
            return new PhoneStateCellInfoCollector(receiver);
    }

    private void updateCollector(Intent intent) {
        if (collector == null)
            collector = createCollector(intent);
        else if (intent != null && intent.getStringExtra(Preferences.PREF_CELLINFO_METHOD) != null) {
            collector.cleanup();
            collector = createCollector(intent);
        }
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
