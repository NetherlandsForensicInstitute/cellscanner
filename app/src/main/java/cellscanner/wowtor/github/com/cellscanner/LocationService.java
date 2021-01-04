package cellscanner.wowtor.github.com.cellscanner;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class LocationService extends Service {
    private static int NOTIFICATION_ERROR = 2;
    private static int NOTIFICATION_STATUS = 3;

    private TelephonyManager mTelephonyManager;
    private Database db;
    private NotificationCompat.Builder mBuilder;

    private static boolean running = false;

    public static void start(Context ctx) {
        running = true;
        ctx.startService(new Intent(ctx, LocationService.class));
    }

    public static void stop(Context ctx) {
        running = false;
        ctx.stopService(new Intent(ctx, LocationService.class));
    }

    public static boolean isRunning() {
        return running;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    public String getDataPath() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(s, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(App.TITLE, "Package name not found", e);
            return "cellinfo.sqlite";
        }

        return p.applicationInfo.dataDir + "/cellinfo.sqlite3";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(App.TITLE, getClass().getName()+".onBind()");
        return null;
    }

    @Override
    public void onCreate() {
        running = true;
        ContextCompat.startForegroundService(this, new Intent(this, LocationService.class));

        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        mBuilder = new NotificationCompat.Builder(this, "default-channel")
                .setContentTitle(App.TITLE)
                .setSmallIcon(cellscanner.wowtor.github.com.cellscanner.R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        startForeground(NOTIFICATION_STATUS, mBuilder.build());

        Log.v(App.TITLE, getClass().getName()+".onCreate()");
        Log.v(App.TITLE, "using db: "+getDataPath());
        db = App.getDatabase();
        db.storePhoneID(getApplicationContext());
        db.storeVersionCode(getApplicationContext());
        Toast.makeText(this, "using db: "+getDataPath(), Toast.LENGTH_SHORT);

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // schedule a periodic update
        final Handler handler = new Handler();
        Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    updateCellInfo();
                    handler.postDelayed(this, App.UPDATE_DELAY_MILLIS);
                }
            }
        };
        handler.post(timer);
    }

    @Override
    public void onDestroy() {
        Log.v(App.TITLE, getClass().getName()+".onDestroy()");

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_STATUS);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel("default-channel", App.TITLE, importance);
            channel.setDescription("notification channel");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(int notification_id, NotificationCompat.Builder mBuilder) {
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        mBuilder = mBuilder
                .setSmallIcon(cellscanner.wowtor.github.com.cellscanner.R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notification_id, mBuilder.build());
    }

    private void sendErrorNotification(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default-channel")
                .setContentTitle("Error")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(sw.toString()))
                .setContentText(e.toString());

        updateNotification(NOTIFICATION_ERROR, mBuilder);
    }

    @SuppressLint("MissingPermission")
    private void updateCellInfo() {
        List<CellInfo> cellinfo = mTelephonyManager.getAllCellInfo();
        String[] cellstr;
        try {
            cellstr = db.storeCellInfo(cellinfo);
            if (cellstr.length == 0)
                cellstr = new String[]{"no data"};
        } catch(Throwable e) {
            Toast.makeText(this, "error: "+e, Toast.LENGTH_SHORT);
            sendErrorNotification(e);
            cellstr = new String[]{"error"};
        }

        Log.v(App.TITLE, "Update cell info: "+TextUtils.join(", ", cellstr));

        mBuilder
                .setContentTitle(String.format("%d cells registered (%d visible)", cellstr.length, cellinfo.size()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(TextUtils.join("\n", cellstr)));

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_STATUS, mBuilder.build());
    }
}
