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
import android.os.Build;
import android.os.IBinder;

import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

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

import nl.nfi.cellscanner.App;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.MainActivity;
import nl.nfi.cellscanner.R;

public class LocationRecordingService extends Service {

    public static final String LOCATION_DATA_UPDATE_BROADCAST= "LOCATION_DATA_UPDATE_MESSAGE";

    private static final String CHANNEL_ID = "ForegroundServiceChannel",
                                SERVICE_TAG = "FOREGROUND_SERVICE_TAG";

    private static final int NOTIF_ID = 123;
    private static final int GPS_LOCATION_INTERVAL = 5;

    private Timer timer;
    private TelephonyManager telephonyManager;
    private Database mDB;
    private NotificationManager notificationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location location;

    @Override
    public void onCreate() {
        super.onCreate();

        /* construct required constants */
        createNotificationChannel();
        timer = new Timer();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mDB = App.getDatabase();
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

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(SERVICE_TAG, "on start command");
        startForeground(NOTIF_ID, getActivityNotification("started"));

        // start the times, schedule for every second
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                preformCellInfoRetrievalRequest();
            }
        }, 0, App.UPDATE_DELAY_MILLIS);

        startLocationUpdates();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // remove the location request timers
        timer.cancel();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.i(SERVICE_TAG, "on destroy");
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
    private void startLocationUpdates() {
        Log.i("LOCATION", "REQUESTING");
        // start the request for location updates
        fusedLocationProviderClient.requestLocationUpdates(
                createLocationRequest(),
                locationCallback,
                null
        );
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
        String[] cellstr;
        try {
            cellstr = mDB.storeCellInfo(cellinfo);
            if (cellstr.length == 0)
                cellstr = new String[]{"no data"};
        } catch(Throwable e) {
            cellstr = new String[]{"error"};
        }
        return cellstr;
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
        } else {
            intent.putExtra("hasLoc", false);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
