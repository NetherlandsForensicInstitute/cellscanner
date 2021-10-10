package nl.nfi.cellscanner.collect;

import android.content.Intent;

public interface DataCollector {
    String[] requiredPermissions(Intent intent);
    void start(Intent intent);
    void stop();
}
