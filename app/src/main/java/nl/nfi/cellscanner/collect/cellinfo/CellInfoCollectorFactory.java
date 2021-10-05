package nl.nfi.cellscanner.collect.cellinfo;

import android.content.Context;
import android.content.Intent;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;

public class CellInfoCollectorFactory extends CollectorFactory {
    @Override
    public String getTitle() {
        return "cell info";
    }

    @Override
    public String getStatusText() {
        return CellscannerApp.getDatabase().getUpdateStatus();
    }

    @Override
    public DataCollector createCollector(Context ctx) {
        return new CellInfoCollector(new DataReceiver(ctx));
    }
}
