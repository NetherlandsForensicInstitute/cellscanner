package nl.nfi.cellscanner.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.PermissionSupport;

public class LocationCollector implements RecordingService.DataCollector {
    private final RecordingService service;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    public LocationCollector(RecordingService service) {
        this.service = service;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(service);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                try {
                    service.registerLocation(locationResult.getLastLocation());
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
        locationRequest.setPriority(Preferences.getLocationAccuracy(service, intent));
        locationRequest.setSmallestDisplacement(CellscannerApp.LOCATION_MINIMUM_DISPLACEMENT_MTRS);
        return locationRequest;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void resume(Context ctx, Intent intent) {
        if (Preferences.isLocationRecordingEnabled(ctx, intent)) {
            // start the request for location updates
            if (PermissionSupport.hasFineLocationPermission(ctx)) {
                fusedLocationProviderClient.requestLocationUpdates(
                        createLocationRequest(intent),
                        locationCallback,
                        null
                );
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void cleanup(Context ctx) {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}
