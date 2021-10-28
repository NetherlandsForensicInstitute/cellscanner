package nl.nfi.cellscanner.collect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.Date;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.collect.cellinfo.TelephonyCellInfoCollector;

public class LocationCollector implements DataCollector {
    private final Context ctx;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    public LocationCollector(Context ctx) {
        this.ctx = ctx;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                try {
                    Factory.storeLocation(locationResult.getLastLocation());
                } catch (Throwable e) {
                    Database.storeMessage(e);
                }
            }
        };

    }

    /**
     * Construct the settings for the location requests used by the app
     * used to configure the fusedLocationProviderClient
     */
    @NotNull
    private LocationRequest createLocationRequest(Intent intent) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(CellscannerApp.LOCATION_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(CellscannerApp.LOCATION_FASTEST_INTERVAL_MILLIS);
        locationRequest.setPriority(Preferences.getLocationAccuracy(ctx, intent));
        locationRequest.setSmallestDisplacement(CellscannerApp.LOCATION_MINIMUM_DISPLACEMENT_MTRS);
        return locationRequest;
    }

    @Override
    public String[] requiredPermissions(Intent intent) {
        return new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
    }

    @SuppressLint("MissingPermission")
    @Override
    public void start(Intent intent) {
        fusedLocationProviderClient.requestLocationUpdates(
                createLocationRequest(intent),
                locationCallback,
                null
        );
    }

    @Override
    public void stop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    public static void createTables(SQLiteDatabase db) {
        Database.createTable(db, "locationinfo", new String[]{
                "provider VARCHAR(200)",
                "accuracy INT",  // accuracy in meters
                "timestamp INT NOT NULL",
                "latitude INT NOT NULL",
                "longitude INT NOT NULL",
                "altitude INT",  // altitude in meters
                "altitude_acc INT",  // altitude accuracy in meters (available in Oreo and up)
                "speed INT",  // speed in meters per second
                "speed_acc INT",  // speed accuracy in meters per second (available in Oreo and up)
                "bearing_deg INT",  // bearing in degrees
                "bearing_deg_acc INT",  // bearing accuracy in degrees (available in Oreo and up)
        });
    }

    public static String getStatusText() {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        SQLiteDatabase db = CellscannerApp.getDatabaseConnection();
        createTables(db);
        try {
            Cursor c = db.rawQuery("SELECT MIN(timestamp), MAX(timestamp), COUNT(*) FROM locationinfo", new String[]{});
            try {
                c.moveToNext();
                long first = c.getLong(0);
                long last = c.getLong(1);
                int count = c.getInt(2);
                long now = new Date().getTime();
                if (count > 0 && now > first) {
                    StringBuffer s = new StringBuffer();
                    s.append(String.format("updated: %s<br/>", fmt.format(last)));
                    s.append(String.format("%d measurements since %d minutes<br/>", count, (now - first) / 1000 / 60));
                    return s.toString();
                } else {
                    return "No measurements.";
                }
            } finally {
                c.close();
            }
        } finally {
            db.close();
        }
    }

    public static class Factory extends CollectorFactory {
        @Override
        public String getTitle() {
            return "location";
        }

        @Override
        public String getStatusText() {
            return LocationCollector.getStatusText();
        }

        @Override
        public DataCollector createCollector(Context ctx) {
            return new LocationCollector(ctx);
        }

        @Override
        public void createTables(SQLiteDatabase db) {
            LocationCollector.createTables(db);
        }

        @Override
        public void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // no upgrade for versions prior to 2
                createTables(db);
                return;
            }

            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE locationinfo ADD COLUMN altitude_acc INT");
                db.execSQL("ALTER TABLE locationinfo ADD COLUMN speed_acc INT");
                db.execSQL("ALTER TABLE locationinfo ADD COLUMN bearing_deg INT");
                db.execSQL("ALTER TABLE locationinfo ADD COLUMN bearing_deg_acc INT");
            }
        }

        @Override
        public void dropDataUntil(SQLiteDatabase db, long timestamp) {
            db.delete("locationinfo", "timestamp <= ?", new String[]{Long.toString(timestamp)});
        }

        public static void storeLocation(Location location) {
            // NOTE: some values (accuracy, speed, altitude) may not be available, but database version 2 and earlier has NOT NULL constraint
            ContentValues values = new ContentValues();
            values.put("provider", location.getProvider());
            values.put("timestamp", location.getTime());
            values.put("accuracy", location.getAccuracy());
            values.put("latitude", location.getLatitude());
            values.put("longitude", location.getLongitude());
            values.put("altitude", location.getAltitude());
            values.put("speed", location.getSpeed());
            if (location.hasBearing())
                values.put("bearing_deg", location.getBearing());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy())
                    values.put("speed_acc", location.getSpeedAccuracyMetersPerSecond());
                if (location.hasVerticalAccuracy())
                    values.put("altitude_acc", location.getVerticalAccuracyMeters());
                if (location.hasBearingAccuracy())
                    values.put("bearing_deg_acc", location.getBearingAccuracyDegrees());
            }

            SQLiteDatabase db = CellscannerApp.getDatabaseConnection();
            try {
                db.insert("locationinfo", null, values);
            } finally {
                db.close();
            }
        }
    }
}
