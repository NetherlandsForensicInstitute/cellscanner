package nl.nfi.cellscanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nl.nfi.cellscanner.recorder.RecorderUtils;
import nl.nfi.cellscanner.upload.Crypto;
import nl.nfi.cellscanner.upload.UploadUtils;

public class UserDataUploadWorker extends Worker {
    private static final String ERROR_CHANNEL_ID = "cellscanner_upload_notification";

    public UserDataUploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static void createNotificationChannel(Context ctx) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ERROR_CHANNEL_ID, "error notification channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManagerCompat.from(ctx).createNotificationChannel(channel);
        }
    }

    private static void notifyError(Context ctx, String title, String message) {
        createNotificationChannel(ctx);

        Intent intent = new Intent(ctx, PreferencesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_symbol24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        // notificationId is a unique int for each notification that you must define
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.notify(0, builder.build());
    }

    private static File createTempFile(Context context) throws IOException {
        File outputDir = context.getCacheDir(); // context being the Activity pointer
        return File.createTempFile("temp-db-", null, outputDir);
    }

    private static void upload(Context ctx, String url_spec) throws Exception {
        String pubkey_pem = Preferences.getMessagePublicKey(ctx);

        long timestamp = new Date().getTime() / 1000L;
        String dest_filename = String.format("%s-%d.sqlite3", Preferences.getInstallID(ctx), timestamp);

        File dbfile = createTempFile(ctx);
        try {
            if (pubkey_pem != null && !pubkey_pem.equals("")) {
                Crypto.encrypt(Database.getDataFile(ctx), dbfile, pubkey_pem);
                dest_filename += ".aes.gz";
            } else {
                Crypto.encrypt(Database.getDataFile(ctx), dbfile, null);
                dest_filename += ".gz";
            }

            InputStream source = new FileInputStream(dbfile);
            try {
                UploadUtils.upload(ctx, url_spec, source, dest_filename);
            } finally {
                source.close();
            }
        } finally {
            dbfile.delete();
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!Database.getDataFile(getApplicationContext()).exists())
            return Result.success();

        Log.i("cellscanner", "Start upload of data file");

        long timestamp = new Date().getTime();
        Context ctx = getApplicationContext();
        try {
            // stop recording
            RecorderUtils.stopService(ctx);

            // upload data
            upload(getApplicationContext(), Preferences.getUploadURL(getApplicationContext()));

            // finish up
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, true, "success", getTags().iterator().next());
            CellScannerApp.getDatabase().dropDataUntil(timestamp);

            return Result.success();
        } catch (java.net.ConnectException e) {
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, "unable to connect", getTags().iterator().next());
            return Result.retry();
        } catch (java.net.SocketTimeoutException e) {
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, "server timeout", getTags().iterator().next());
            return Result.retry();
        } catch (Exception e) {
            notifyError(getApplicationContext(), "Cellscanner upload error", e.getMessage());
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, e.toString(), getTags().iterator().next());
            return Result.retry();
        } finally {
            // resume recording
            RecorderUtils.applyRecordingPolicy(ctx);
        }
    }

    private static void scheduleWorkRequest(Context ctx, WorkRequest workRequest) {
        WorkManager
                .getInstance(ctx)
                .enqueue(workRequest);
    }

    public static void applyUploadPolicy(Context ctx, boolean do_periodic_upload, boolean unmetered_only) {
        unSchedulePeriodDataUpload(ctx);
        if (do_periodic_upload)
            schedulePeriodicDataUpload(ctx, unmetered_only);
    }

    public static void applyUploadPolicy(Context ctx) {
        applyUploadPolicy(ctx, Preferences.getAutoUploadEnabled(ctx), Preferences.getUnmeteredUploadOnly(ctx));
    }

    @NotNull
    private static Constraints getWorkManagerConstraints(Context ctx, boolean unmetered_only) {
        NetworkType networkType = unmetered_only ? NetworkType.UNMETERED : NetworkType.CONNECTED;
        return new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();
    }

    public static void startDataUpload(final Context ctx) {
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest
                .Builder(UserDataUploadWorker.class)
                .addTag(ExportResultRepository.MANUAL)
                .build();

        Toast.makeText(ctx, "Uploading data", Toast.LENGTH_LONG).show();
        scheduleWorkRequest(ctx, uploadWorkRequest);
    }

    /**
     * Schedules a Periodic Upload of the data
     */
    private static void schedulePeriodicDataUpload(Context ctx, boolean unmetered_only) {
        Constraints constraints = getWorkManagerConstraints(ctx, unmetered_only);

        PeriodicWorkRequest uploadWorkRequest = new PeriodicWorkRequest
                .Builder(UserDataUploadWorker.class, CellScannerApp.UPLOAD_INTERVAL_MINUTES, TimeUnit.MINUTES) // upload every 15 minutes
                .addTag(ExportResultRepository.AUTO)
                .setConstraints(constraints)
                .build();

        scheduleWorkRequest(ctx, uploadWorkRequest);
    }

    private static void unSchedulePeriodDataUpload(Context ctx) {
        WorkManager
                .getInstance(ctx)
                .cancelAllWorkByTag(ExportResultRepository.AUTO);
    }
}
