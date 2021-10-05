package nl.nfi.cellscanner.collect;

import android.content.Context;
import android.content.Intent;

public abstract class CollectorFactory {
    public abstract String getTitle();
    public abstract String getStatusText();

    public String[] requiredPermissions(Context ctx, Intent intent) {
        return createCollector(ctx).requiredPermissions(intent);
    }

    public abstract DataCollector createCollector(Context ctx);
}
