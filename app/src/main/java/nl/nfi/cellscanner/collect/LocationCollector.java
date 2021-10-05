package nl.nfi.cellscanner.collect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

public class LocationCollector implements DataCollector {
    private final DataReceiver service;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    public LocationCollector(DataReceiver service) {
        this.service = service;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(service.getContext());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                try {
                    service.storeLocation(locationResult.getLastLocation());
                } catch (Throwable e) {
                    CellscannerApp.getDatabase().storeMessage(e);
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
        locationRequest.setPriority(Preferences.getLocationAccuracy(service.getContext(), intent));
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
    public void resume(Intent intent) {
        fusedLocationProviderClient.requestLocationUpdates(
                createLocationRequest(intent),
                locationCallback,
                null
        );
    }

    @Override
    public void cleanup() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private static LocationDatabase getDatabase() {
        return new LocationDatabase(CellscannerApp.getDatabaseConnection());
    }

    private static class LocationDatabase {
        private final SQLiteDatabase db;

        public LocationDatabase(SQLiteDatabase db) {
            this.db = db;
        }

        public void createTables() {
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

        public String getStatusText() {
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

            createTables();
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
        }
    }

    public static class Factory extends CollectorFactory {
        @Override
        public String getTitle() {
            return "location";
        }

        @Override
        public String getStatusText() {
            return getDatabase().getStatusText();
        }

        @Override
        public DataCollector createCollector(Context ctx) {
            return new LocationCollector(new DataReceiver(ctx));
        }

        @Override
        public void createTables(SQLiteDatabase db) {
            new LocationDatabase(db).createTables();
        }

        @Override
        public void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // no upgrade for versions prior to 2
                new LocationDatabase(db).createTables();
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
    }
}
