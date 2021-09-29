package nl.nfi.cellscanner.collect;

import android.content.Context;
import android.content.Intent;

public interface DataCollector {
    String[] requiredPermissions();
    void resume(Context ctx, Intent intent);
    void cleanup(Context ctx);
}
