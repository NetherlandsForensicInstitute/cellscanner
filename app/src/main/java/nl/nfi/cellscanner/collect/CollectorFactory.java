package nl.nfi.cellscanner.collect;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

public abstract class CollectorFactory {
    public abstract String getTitle();
    public abstract String getStatusText();

    public String[] requiredPermissions(Context ctx, Intent intent) {
        return createCollector(ctx).requiredPermissions(intent);
    }

    public abstract DataCollector createCollector(Context ctx);
    public abstract void createTables(SQLiteDatabase db);
    public abstract void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion);
    public abstract void dropDataUntil(SQLiteDatabase db, long timestamp);
}
