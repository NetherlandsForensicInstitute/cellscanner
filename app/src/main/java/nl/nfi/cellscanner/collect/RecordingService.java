package nl.nfi.cellscanner.collect;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.Database;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.ViewMeasurementsActivity;
import nl.nfi.cellscanner.R;
import nl.nfi.cellscanner.PermissionSupport;

/**
 * Service responsible for recording the Location data and storing it in the database
 * */
public class RecordingService extends Service {

    private static final String RECORDING_STATUS_CHANNEL = "RECORDING_STATUS_CHANNEL";
    private static final String ACTION_REQUIRED_CHANNEL = "ACTION_REQUIRED_CHANNEL";
    private static final int STATUS_NOTIFICATION_ID = 1;
    private static final int PERMISSION_NOTIFICATION_ID = 2;

    private NotificationManager notificationManager;
    private final Map<String,DataCollector> collectors = new HashMap<>();
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(STATUS_NOTIFICATION_ID, getActivityNotification("started"));

        /* construct required constants */
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        /* store some constants in the database */
        Database db = CellscannerApp.getDatabase();
        db.storeInstallID(getApplicationContext());
        db.storeVersionCode(getApplicationContext());

        PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cellscanner::RecordingWakelock");
        wakeLock.acquire();
    }

    /**
     * start or stop recording GPS data based on the app state
     */
    @SuppressLint("MissingPermission")
    protected synchronized void updateRecordingState(Intent intent) {
        for (String name : Preferences.COLLECTORS) {
            if (Preferences.isRecordingEnabled(this, intent) && Preferences.isCollectorEnabled(name, this, intent)) {
                if (collectors.containsKey(name))
                    collectors.get(name).resume(intent);
                else {
                    DataCollector collector = CellscannerApp.createCollector(name, new DataReceiver(this));
                    collectors.put(name, collector);
                    collector.resume(intent);
                }
            } else {
                if (collectors.containsKey(name)) {
                    collectors.get(name).cleanup();
                    collectors.remove(name);
                }
            }
        }

        if (!notifyPermissionRequired()) {
            try {
                String msg = Preferences.isRecordingEnabled(this, intent) ? "recording" : "idle";
                notificationManager.notify(
                        STATUS_NOTIFICATION_ID,
                        getActivityNotification(msg)
                );
                ViewMeasurementsActivity.refresh(this);
            } catch (Throwable e) {
                CellscannerApp.getDatabase().storeMessage(e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the application should start recording GPS
        updateRecordingState(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (DataCollector collector : collectors.values()) {
            collector.cleanup();
        }
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
}
