package nl.nfi.cellscanner.recorder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

import nl.nfi.cellscanner.CellScannerApp;
import nl.nfi.cellscanner.CellStatus;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.ViewMeasurementsActivity;
import nl.nfi.cellscanner.R;

/**
 * Service responsible for recording the Location data and storing it in the database
 * */
public class LocationRecordingService extends Service {

    public static final String LOCATION_DATA_UPDATE_BROADCAST = "LOCATION_DATA_UPDATE_MESSAGE";

    private static final String RECORDING_STATUS_CHANNEL = "RECORDING_STATUS_CHANNEL";
    private static final String ACTION_REQUIRED_CHANNEL = "ACTION_REQUIRED_CHANNEL";
    private static final int STATUS_NOTIFICATION_ID = 1;
    private static final int PERMISSION_NOTIFICATION_ID = 2;

    private Database mDB;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location location;
    private LocationCallback locationCallback;
    private NotificationManager notificationManager;
    private LegacyPhoneState phone_state;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(STATUS_NOTIFICATION_ID, getActivityNotification("started"));

        /* construct required constants */
        mDB = CellScannerApp.getDatabase();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        /* store some constants in the database */
        mDB.storeInstallID(getApplicationContext());
        mDB.storeVersionCode(getApplicationContext());

        /*
            initialize a callback function that listens for location updates
            made by the (GPS) location manager
         */
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                try {
                    processLocationUpdate(locationResult.getLastLocation());
                } catch (Throwable e) {
                    CellScannerApp.getDatabase().storeMessage(e);
                }
            }
        };

        phone_state = new LegacyPhoneState(this);

        PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cellscanner::RecordingWakelock");
        wakeLock.acquire();
    }

    /**
     * start or stop recording GPS data based on the app state
     * @param ctx: Context of the running service
     */
    @SuppressLint("MissingPermission")
    protected synchronized void updateRecordingState(Context ctx, Intent intent) {
        if (Preferences.isLocationRecordingEnabled(ctx, intent)) {
            // start the request for location updates
            if (PermissionSupport.hasFineLocationPermission(getApplicationContext())) {
                fusedLocationProviderClient.requestLocationUpdates(
                        createLocationRequest(intent),
                        locationCallback,
                        null
                );
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        phone_state.update(ctx, intent);

        if (!notifyPermissionRequired()) {
            try {
                String msg = Preferences.isRecordingEnabled(ctx, intent) ? "recording" : "idle";
                notificationManager.notify(
                        STATUS_NOTIFICATION_ID,
                        getActivityNotification(msg)
                );
                sendBroadcastMessage();
            } catch (Throwable e) {
                CellScannerApp.getDatabase().storeMessage(e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the application should start recording GPS
        updateRecordingState(getApplicationContext(), intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        phone_state.cleanup(getApplicationContext());
        wakeLock.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Build the notification related to the application
     * @param text: Text to show in the notification
     * @return: Notification to show
     */
    private Notification getActivityNotification(String text) {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, ViewMeasurementsActivity.class), 0);

        return new NotificationCompat.Builder(this, RECORDING_STATUS_CHANNEL)
                .setContentTitle("Cellscanner")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_symbol24)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW) // forground service requires priority "low" or more
                .build();

    }

    /**
     * Build notification channel related to the application
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel serviceChannel = new NotificationChannel(
                    RECORDING_STATUS_CHANNEL,
                    "Recording status",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(serviceChannel);

            NotificationChannel actionChannel = new NotificationChannel(
                    ACTION_REQUIRED_CHANNEL,
                    "Action required",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(actionChannel);
        }
    }

    /**
     * Construct the settings for the location requests used by the app
     * used to configure the fusedLocationProviderClient
     */
    @NotNull
    private LocationRequest createLocationRequest(Intent intent) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(CellScannerApp.LOCATION_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(CellScannerApp.LOCATION_FASTEST_INTERVAL_MILLIS);
        locationRequest.setPriority(Preferences.getLocationAccuracy(this, intent));
        locationRequest.setSmallestDisplacement(CellScannerApp.LOCATION_MINIMUM_DISPLACEMENT_MTRS);
        return locationRequest;
    }

    private boolean notifyPermissionRequired() {
        List<String> missing_permissions = PermissionSupport.getMissingPermissions(getApplication(), null);
        if (missing_permissions.isEmpty()) {
            notificationManager.cancel(PERMISSION_NOTIFICATION_ID);
            return false;
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(this, ACTION_REQUIRED_CHANNEL)
                    .setContentTitle("Cellscanner")
                    .setContentText("Cellscanner requires permission to access device " + TextUtils.join(" and ", missing_permissions))
                    .setSmallIcon(R.drawable.ic_symbol24)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            notificationManager.notify(PERMISSION_NOTIFICATION_ID, notification);

            return true;
        }
    }

    public void registerCellStatus(String subscription, Date date_start, Date date_end, CellStatus status) {
        mDB.updateCellStatus(subscription, date_start, date_end, status);
        refreshMeasurementsView();
    }


    /**
     * Processes the GPS location update.
     *
     * Store the location in the App database and trigger broadcast message
     * to all listening parties
     *
     * @param lastLocation: Location object received
     */
    private void processLocationUpdate(Location lastLocation) {
        location = lastLocation;
        // store it in the database
        mDB.storeLocationInfo(location);
        sendBroadcastMessage();
    }

    public void registerCallState(String subscription, int state) {
        // store it in the database
        mDB.storeCallState(state);
    }

    /**
     * Broadcast an intent, communicating last captured location related data (GPS)
     * TODO: Might extend with Cell data
     */
    private void sendBroadcastMessage() {
        Intent intent = new Intent(LOCATION_DATA_UPDATE_BROADCAST);

        /* Wrap updated location information in the Intent */
        if (location != null) {
            intent.putExtra("hasLoc", true);
            intent.putExtra("lon", location.getLongitude());
            intent.putExtra("lat", location.getLatitude());
            intent.putExtra("lts", location.getTime());
            if (location.hasAccuracy())
                intent.putExtra("acc", location.getAccuracy());
            intent.putExtra("pro", location.getProvider());
            if (location.hasAltitude())
                intent.putExtra("alt", location.getAltitude());
            if (location.hasSpeed())
                intent.putExtra("spd", location.getSpeed());
            if (location.hasBearing())
                intent.putExtra("bearing", location.getBearing());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasBearingAccuracy())
                    intent.putExtra("bearing_acc", location.getBearingAccuracyDegrees());
                if (location.hasSpeedAccuracy())
                    intent.putExtra("spd_acc", location.getSpeedAccuracyMetersPerSecond());
                if (location.hasVerticalAccuracy())
                    intent.putExtra("alt_acc", location.getVerticalAccuracyMeters());
            }
        } else {
            intent.putExtra("hasLoc", false);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void refreshMeasurementsView() {
        Intent intent = new Intent(LOCATION_DATA_UPDATE_BROADCAST);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
