package nl.nfi.cellscanner.collect;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.ViewMeasurementsActivity;

public class TrafficCollector implements DataCollector {
    private static final String DOWNLOAD_URL = "https://download.xs4all.nl/test/1MB.bin";
    private static final String TABLE_NAME = "ip_traffic";

    private final Context ctx;
    private WorkManager workManager = null;

    public TrafficCollector(Context ctx) {
        this.ctx = ctx;
    }

    public static void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        new TrafficDatabase(db).createTables();
    }

    @Override
    public String[] requiredPermissions(Intent intent) {
        return new String[0];
    }

    @Override
    public void start(Intent intent) {
        if (workManager == null) {
            workManager = WorkManager.getInstance(ctx);
            workManager.enqueue(new PeriodicWorkRequest.Builder(Downloader.class, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS).build());
        }
    }

    @Override
    public void stop() {
        if (workManager != null) {
            workManager.cancelAllWork();
            workManager = null;
        }
    }

    private static TrafficDatabase getDatabase() {
        return new TrafficDatabase(CellscannerApp.getDatabaseConnection());
    }

    private static class TrafficDatabase {
        private final SQLiteDatabase db;

        public TrafficDatabase(SQLiteDatabase db) {
            this.db = db;
        }

        public void createTables() {
            Database.createTable(db, TABLE_NAME, new String[]{
                    "date_start INT NOT NULL",
                    "date_end INT",
                    "bytes_read INT NOT NULL",
            });
        }

        public void updateRecord(Context ctx, Date date_start, Date date_end, int bytes_read) {
            ContentValues values = new ContentValues();
            values.put("date_end", date_end != null ? date_end.getTime() : -1);
            int nrows = db.update(TABLE_NAME, values, "date_start = ?", new String[]{Long.toString(date_start.getTime())});
            if (nrows == 0) {
                values.put("date_start", date_start.getTime());
                values.put("bytes_read", bytes_read);
                db.insert(TABLE_NAME, null, values);
                Log.v(CellscannerApp.TITLE, "new record: traffic: "+values.toString());
            }

            ViewMeasurementsActivity.refresh(ctx);
        }

        public String getStatusText() {
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

            Cursor cur = db.rawQuery("SELECT MIN(date_start), MAX(date_end), COUNT(*) FROM "+TABLE_NAME+" WHERE date_end is NOT NULL", new String[]{});
            try {
                cur.moveToNext();
                long first = cur.getLong(0);
                long last = cur.getLong(1);
                int count = cur.getInt(2);
                long now = new Date().getTime();
                if (count > 0 && now > first) {
                    StringBuffer s = new StringBuffer();
                    s.append(String.format("updated: %s<br/>", fmt.format(last)));
                    s.append(String.format("%d downloads since %d minutes<br/>", count, (now - first) / 1000 / 60));
                    return s.toString();
                } else {
                    return "No data.";
                }
            } finally {
                cur.close();
            }
        }
    }

    public static class Downloader extends Worker {
        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        public Downloader(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
            super(appContext, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Date date_start = new Date();
            getDatabase().updateRecord(getApplicationContext(), date_start, null, 0);
            try {
                URLConnection con = new URL(DOWNLOAD_URL).openConnection();
                InputStream is = con.getInputStream();
                byte[] buf = new byte[4096];
                int bytes_read = 0;
                for (;;) {
                    int n = is.read(buf);
                    if (n == -1)
                        break;
                    else
                        bytes_read += n;
                }

                getDatabase().updateRecord(getApplicationContext(), date_start, new Date(), bytes_read);
                return Result.success();
            } catch (Exception e) {
                Database.storeMessage(e);
                return Result.failure();
            }
        }
    }

    public static class Factory extends CollectorFactory {
        @Override
        public String getTitle() {
            return "traffic";
        }

        @Override
        public String getStatusText() {
            return getDatabase().getStatusText();
        }

        @Override
        public DataCollector createCollector(Context ctx) {
            return new TrafficCollector(ctx);
        }

        @Override
        public void createTables(SQLiteDatabase db) {
            new TrafficDatabase(db).createTables();
        }

        @Override
        public void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            new TrafficDatabase(db).createTables();
        }

        @Override
        public void dropDataUntil(SQLiteDatabase db, long timestamp) {
            db.delete(TABLE_NAME, "date_start <= ?", new String[]{Long.toString(timestamp)});
        }
    }
}
