package nl.nfi.cellscanner.collect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.PermissionSupport;

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
    public String[] requiredPermissions() {
        return new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
    }

    @SuppressLint("MissingPermission")
    @Override
    public void resume(Context ctx, Intent intent) {
        if (PermissionSupport.hasPermissions(ctx, requiredPermissions())) {
            fusedLocationProviderClient.requestLocationUpdates(
                    createLocationRequest(intent),
                    locationCallback,
                    null
            );
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void cleanup(Context ctx) {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}
