package nl.nfi.cellscanner;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.Locale;

import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.LocationCollector;
import nl.nfi.cellscanner.collect.RecordingService;
import nl.nfi.cellscanner.collect.SubscriptionDataCollector;
import nl.nfi.cellscanner.collect.phonestate.PhoneStateFactory;

/**
 * Main application state
 */
public class CellscannerApp extends Application {
    public static final String TITLE = "cellscanner";

    // auto upload interval
    public static int UPLOAD_INTERVAL_MINUTES = 55*7;

    // interval for requesting locations
    public static final int LOCATION_INTERVAL_MILLIS = 5000;

    // interval for accepting locations if they happen to be available (may be less than LOCATION_INTERVAL_MILLIS)
    public static final int LOCATION_FASTEST_INTERVAL_MILLIS = 2500;

    // minimum displacement before logging a location
    public static final float LOCATION_MINIMUM_DISPLACEMENT_MTRS = 50;

    public static final String MQTT_DEFAULT_TOPIC = "/cellscanner.db";

    private static SQLiteOpenHelper dbhelper;
    private static final int DATABASE_VERSION = Database.VERSION;

    /**
     * get or create a database to use within the application
     * @return Database
     */
    public static Database getDatabase() {
        return new Database(dbhelper.getWritableDatabase());
    }

    /**
     * Clear the existing database
     * @param appcontext
     */
    public static void resetDatabase(Context appcontext) {
        // close the existing database connection
        dbhelper.close();
        File path = Database.getDataFile(appcontext);
        Log.i("App", String.format(Locale.ROOT, "deleting database at path: %s", path.toString()));
        appcontext.deleteDatabase(path.toString());
        dbhelper = new OpenHelper(appcontext);
    }

    /**
     * A helper class to manage database creation and version management.
     * This class makes it easy for ContentProvider implementations to defer opening and upgrading
     * the database until first use, to avoid blocking application startup with long-running database
     * upgrades.
     */
    private static class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context) {
            super(context, Database.getDataFile(context).toString(), null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            Database.createTables(sqLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            Database.upgrade(sqLiteDatabase, oldVersion, newVersion);
        }
    }

    @Override
    public void onCreate() {
        Log.i("App", "created");
        super.onCreate();
        dbhelper = new OpenHelper(getApplicationContext());
    }

    public static String getVersionName(Context ctx) {
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static DataCollector createCollector(String name, DataReceiver receiver) {
        if (name.equals(Preferences.PREF_CELLINFO_RECORDING)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return new SubscriptionDataCollector(receiver, new PhoneStateFactory.CellInfoFactory());
            } else {
                return new SubscriptionDataCollector(receiver, new PhoneStateFactory.CellInfoFactory());
                // TODO: test API level 31+
                //factory = new CellInfoFactory();
            }
        }

        if (name.equals(Preferences.PREF_CALL_STATE_RECORDING)) {
            SubscriptionDataCollector.SubscriptionCallbackFactory factory;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                factory = new PhoneStateFactory.CallStateFactory();
            } else {
                factory = new PhoneStateFactory.CallStateFactory();
                // TODO: test API level 31+
                //factory = new CallStateFactory();
            }

            return new SubscriptionDataCollector(receiver, factory);
        }

        if (name.equals(Preferences.PREF_LOCATION_RECORDING)) {
            return new LocationCollector(receiver);
        }

        throw new RuntimeException("no such collector: "+name);
    }
}
