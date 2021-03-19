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

import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    /* Settings for storing GPS related data */
    private static final int GPS_LOCATION_INTERVAL = 5; // requested interval in seconds
    private static final float SMALLEST_DISPLACEMENT_BEFORE_LOGGING_MTRS = 50;

    private Database mDB;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location location;
    private LocationCallback locationCallback;
    private PhoneStateListener phoneStateCallback;
    private TelephonyManager telephonyManager;
    private NotificationManager notificationManager;
    private Timer timer; // Make a cell scan on every tick

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(STATUS_NOTIFICATION_ID, getActivityNotification("started"));

        /* construct required constants */
        timer = new Timer();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mDB = CellScannerApp.getDatabase();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        /* store some constants in the database */
        mDB.storeInstallID(getApplicationContext());
        mDB.storeVersionCode(getApplicationContext());

        // start the times, schedule for every second
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                preformCellInfoRetrievalRequest();
            }
        }, CellScannerApp.UPDATE_DELAY_MILLIS, CellScannerApp.UPDATE_DELAY_MILLIS);

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

        phoneStateCallback = new PhoneStateListener() {
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                try {
                    processCallStateUpdate(state);
                } catch (Throwable e) {
                    CellScannerApp.getDatabase().storeMessage(e);
                }
            }
        };
    }

    /**
     * start or stop recording GPS data based on the app state
     * @param ctx: Context of the running service
     */
    @SuppressLint("MissingPermission")
    private void updateRecordingState(Context ctx) {
        if (Preferences.isLocationRecordingEnabled(ctx)) {
            // start the request for location updates
            if (PermissionSupport.hasFineLocationPermission(getApplicationContext())) {
                fusedLocationProviderClient.requestLocationUpdates(
                        createLocationRequest(),
                        locationCallback,
                        null
                );
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        if (Preferences.isCallStateRecordingEnabled(ctx)) {
            // start the request for location updates
            if (PermissionSupport.hasCallStatePermission(getApplicationContext())) {
                telephonyManager.listen(phoneStateCallback, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } else {
            telephonyManager.listen(phoneStateCallback, PhoneStateListener.LISTEN_NONE);
        }

        notifyPermissionRequired();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the application should start recording GPS
        updateRecordingState(getApplicationContext());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // remove the location request timers & updates
        timer.cancel();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        telephonyManager.listen(phoneStateCallback, PhoneStateListener.LISTEN_NONE);
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

    private static int recordingPriorityValue(Context context) {
        return Preferences.isHighPrecisionRecordingEnabled(context) ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_NO_POWER;
    }

    /**
     * Construct the settings for the location requests used by the app
     * used to configure the fusedLocationProviderClient
     */
    @NotNull
    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000 * GPS_LOCATION_INTERVAL);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(recordingPriorityValue(this));
        locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT_BEFORE_LOGGING_MTRS);
        return locationRequest;
    }

    private void notifyPermissionRequired() {
        List<String> missing_permissions = PermissionSupport.getMissingPermissions(getApplication());
        if (missing_permissions.isEmpty())
            notificationManager.cancel(PERMISSION_NOTIFICATION_ID);
        else {
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
        }
    }

    @SuppressLint("MissingPermission")
    private List<CellInfo> getCellInfo() {
        /*
          - This code should not run if the permissions are not there
          - Code should check and ask for permissions when the 'start recording switch' in the main activity
            is switched to start running when the permissions are not there
         */
        if (PermissionSupport.hasCourseLocationPermission(getApplicationContext())) {
            notificationManager.cancel(PERMISSION_NOTIFICATION_ID);  // cancel permissions warning if any
            return telephonyManager.getAllCellInfo();
        } else {
            notifyPermissionRequired();
            return new ArrayList<>();
        }

    }

    private List<CellStatus> storeCellInfo(List<CellInfo> cellinfo) {
        /*
        // TODO: Be more clear around this

        This code does not store the records, this code;
        - creates new records
        - updates already stored records
        - turns modified records in a string and reports them back

         */
        List<CellStatus> cells = new ArrayList<>();
        for (CellInfo info : cellinfo) {
            try {
                CellStatus status = CellStatus.fromCellInfo(info);
                if (status.isValid())
                    cells.add(status);
            } catch (CellStatus.UnsupportedTypeException e) {
                mDB.storeMessage(e);
            }
        }

        mDB.updateCellStatus(cells);

        return cells;
    }


    /**
     * Retrieve the current CellInfo, update;
     * - database
     * - Service notification
     * - send broadcast to update App
     */
    private void preformCellInfoRetrievalRequest() {
        try {
            List<CellInfo> cellinfo = getCellInfo();
            List<CellStatus> cellstr = storeCellInfo(cellinfo);
            notificationManager.notify(
                    STATUS_NOTIFICATION_ID,
                    getActivityNotification(String.format("%d cells registered (%d visible)", cellstr.size(), cellinfo.size()))
            );
            sendBroadcastMessage();
        } catch (Throwable e) {
            CellScannerApp.getDatabase().storeMessage(e);
        }
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

    private void processCallStateUpdate(int state) {
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
            intent.putExtra("acc", location.getAccuracy());
            intent.putExtra("pro", location.getProvider());
            intent.putExtra("alt", location.getAltitude());
            intent.putExtra("spd", location.getSpeed());
        } else {
            intent.putExtra("hasLoc", false);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
