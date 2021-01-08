package cellscanner.wowtor.github.com.cellscanner.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import cellscanner.wowtor.github.com.cellscanner.App;
import cellscanner.wowtor.github.com.cellscanner.MainActivity;
import cellscanner.wowtor.github.com.cellscanner.R;

public class ForegroundService extends Service {

    private static final int NOTIF_ID = 123;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String SERVICE_TAG = "FOREGROUND_SERVICE_TAG";
    private Timer mTimer;
    private TelephonyManager mTelephonyManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mTimer = new Timer();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(SERVICE_TAG, "on start command");
        startForeground(NOTIF_ID, getActivityNotification("started"));

        // start the times, schedule for every second
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                NotificationManager mngr =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mngr.notify(NOTIF_ID, getActivityNotification("massive"));
            }
        }, 0, App.UPDATE_DELAY_MILLIS);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
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
}
