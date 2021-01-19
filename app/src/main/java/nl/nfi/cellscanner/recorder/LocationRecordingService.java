package nl.nfi.cellscanner.recorder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import android.telephony.CellInfo;
import android.telephony.TelephonyManager;

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
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nl.nfi.cellscanner.CellScannerApp;
import nl.nfi.cellscanner.CellStatus;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.MainActivity;
import nl.nfi.cellscanner.R;

public class LocationRecordingService extends Service {

    public static final String LOCATION_DATA_UPDATE_BROADCAST= "LOCATION_DATA_UPDATE_MESSAGE";

    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    private static final int NOTIF_ID = 123;

    /* Settings for storing GPS related data */
    private static final int GPS_LOCATION_INTERVAL = 5; // requested interval in seconds

    private Timer timer;
    private TelephonyManager telephonyManager;
    private Database mDB;
    private NotificationManager notificationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location location;
    private BroadcastReceiver gpsRecorderListener;

    @Override
    public void onCreate() {
        super.onCreate();

        /* construct required constants */
        createNotificationChannel();
        timer = new Timer();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mDB = CellScannerApp.getDatabase();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // TODO: FIND OUT WHY HERE
        mDB.storePhoneID(getApplicationContext());
        mDB.storeVersionCode(getApplicationContext());

        // initialize a callback function that listens for location updates
        locationCallback  = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                processLocationUpdate(locationResult.getLastLocation());
            }
        };


        gpsRecorderListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                toggleGPSRecording(context);
            }
        };

    }

    private void toggleGPSRecording(Context ctx) {
        if (Recorder.gpsRecordingState(ctx)) startGPSLocationUpdates();
        else stopGPSLocationUpdates();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, getActivityNotification("started"));

        // start the times, schedule for every second
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                preformCellInfoRetrievalRequest();
            }
        }, 0, CellScannerApp.UPDATE_DELAY_MILLIS);

        // Start listening for updates on the record GPS switch
        LocalBroadcastManager.getInstance(this).registerReceiver(gpsRecorderListener, new IntentFilter(MainActivity.RECORD_GPS));

        // Check if the application should start recording GPS
        toggleGPSRecording(getApplicationContext());

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // remove the location request timers & updates
        timer.cancel();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsRecorderListener);
        stopGPSLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification getActivityNotification(String text) {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Cellscanner")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_symbol24)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
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
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }


    @SuppressLint("MissingPermission")
    private void startGPSLocationUpdates() {
        // start the request for location updates
        fusedLocationProviderClient.requestLocationUpdates(
                createLocationRequest(),
                locationCallback,
                null
        );
    }

    private void stopGPSLocationUpdates() {
        // stop the active request for location updates
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    @SuppressLint("MissingPermission") // permission check is moved to another part of the app
    private List<CellInfo> getCellInfo() {
        /*
          - This code should not run if the permissions are not there
          - Code should check and ask for permissions when the 'start recording switch' in the main activity
            is switched to start running when the permissions are not there
         */
        if (PermissionSupport.hasAccessCourseLocationPermission(getApplicationContext())) {
            return telephonyManager.getAllCellInfo();
        } else {
            // TODO: Shutdown this service ...???
            /*
            Can only get in this situation when the location permission is revoked
            Should spawn a notification and kill this is service
             */
            return new ArrayList<>();
        }

    }

    private String[] storeCellInfo(List<CellInfo> cellinfo) {
        /*
        // TODO: Be more clear around this

        This code does not store the records, this code;
        - creates new records
        - updates already stored records
        - turns modified records in a string and reports them back

         */
        Date date = new Date();

        List<String> cells = new ArrayList<>();
        for (CellInfo info : cellinfo) {
            try {
                CellStatus status = CellStatus.fromCellInfo(info);
                if (status.isValid()) {
                    mDB.updateCellStatus(date, status);
                    cells.add(status.toString());
                }
            } catch (CellStatus.UnsupportedTypeException e) {
                mDB.storeMessage(e.getMessage());
            }
        }

        return cells.toArray(new String[0]);
    }


    /**
     * Retrieve the current CellInfo, update;
     * - database
     * - Service notification
     * - send broadcast to update App
     */
    private void preformCellInfoRetrievalRequest() {
        List<CellInfo> cellinfo = getCellInfo();
        String[] cellstr = storeCellInfo(cellinfo);
        notificationManager.notify(
                NOTIF_ID,
                getActivityNotification(String.format("%d cells registered (%d visible)", cellstr.length, cellinfo.size()))
        );
        sendBroadcastMessage();
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
        mDB.storeLocationInfo(location);
        // store it in the database
        sendBroadcastMessage();
    }

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
