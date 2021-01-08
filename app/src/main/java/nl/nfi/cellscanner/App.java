package nl.nfi.cellscanner;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class App extends Application {
    public static final String TITLE = "cellscanner";
    public static int UPDATE_DELAY_MILLIS = 1000;
    public static int EVENT_VALIDITY_MILLIS = UPDATE_DELAY_MILLIS+20000;

    private static SQLiteOpenHelper dbhelper;

    private static final int DATABASE_VERSION = Database.VERSION;

    public static Database getDatabase() {
        return new Database(dbhelper.getWritableDatabase());
    }

    public static void resetDatabase(Context appcontext) {
        dbhelper.close();

        File path = Database.getDataPath(appcontext);
        path.delete();

        dbhelper = new OpenHelper(appcontext);
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context) {
            super(context, Database.getDataPath(context).toString(), null, DATABASE_VERSION);
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
        super.onCreate();
        dbhelper = new OpenHelper(getApplicationContext());
    }
}
