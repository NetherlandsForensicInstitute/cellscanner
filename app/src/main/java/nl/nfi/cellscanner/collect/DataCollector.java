package nl.nfi.cellscanner.collect;

import android.content.Intent;

public interface DataCollector {
    String[] requiredPermissions(Intent intent);
    void resume(Intent intent);
    void cleanup();
}
