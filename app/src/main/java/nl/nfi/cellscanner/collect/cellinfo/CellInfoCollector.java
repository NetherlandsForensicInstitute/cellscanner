package nl.nfi.cellscanner.collect.cellinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.DataCollector;

public class CellInfoCollector implements DataCollector {
    private final Context ctx;
    private DataCollector collector = null;

    public CellInfoCollector(Context ctx) {
        this.ctx = ctx;
    }

    private DataCollector createCollector(Intent intent) {
        String method = Preferences.getCellInfoMethod(ctx, intent);

        if (method.equals("AUTO")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                method = "TELEPHONY_CALLBACK";
            } else {
                method = "PHONE_STATE";
            }
        }

        if (method.equals("TELEPHONY_CALLBACK") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return new TelephonyCellInfoCollector(ctx);
        else if (method.equals("PHONE_STATE"))
            return new PhoneStateCellInfoCollector(ctx);
        else if (method.equals("BASIC"))
            return new TimerCellInfoCollector(ctx);
        else
            return new PhoneStateCellInfoCollector(ctx);
    }

    private void updateCollector(Intent intent) {
        if (collector == null)
            collector = createCollector(intent);
        else if (intent != null && intent.getStringExtra(Preferences.PREF_CELLINFO_METHOD) != null) {
            collector.stop();
            collector = createCollector(intent);
        }
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
        if (collector != null) {
            collector.stop();
            collector = null;
        }
    }
}
