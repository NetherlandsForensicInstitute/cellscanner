package cellscanner.wowtor.github.com.cellscanner.recorder;

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
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cellscanner.wowtor.github.com.cellscanner.MainActivity;
import cellscanner.wowtor.github.com.cellscanner.R;

public class ForegroundService extends Service {

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
        String input = intent.getStringExtra("inputExtra");

        createNotificationChannel();

        // what to start when you click the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_symbol24)
                .setContentIntent(pendingIntent);
//                .build();

        startForeground(1, notification.build());

        // start the times, schedule for every second
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

            }
        }, 0, 10000);

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
}
