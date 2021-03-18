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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.nfi.cellscanner.recorder.RecorderUtils;

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

    private static void uploadFtp(Context ctx, InputStream source, String dest_filename, String host, String user, String password) throws IOException {
        FileInputStream fileInputStream;
        FTPClient con = new FTPClient();

        con.connect(host);
        if (con.login(user, password))
        {
            con.enterLocalPassiveMode(); // important!
            con.setFileType(FTP.BINARY_FILE_TYPE);

            // get the file and send it. A
            boolean result = con.storeFile(dest_filename, source);
            source.close();

            if (result) {
                Log.e("cellscanner", "upload result: succeeded");

                /*when file has been uploaded, the old data can be flushed*/

            } else {
                Log.e("cellscanner", "upload result: Failed");
            }

            // disconnect from the server
            con.logout();
            con.disconnect();
        }
    }

    private static void uploadSftp(Context ctx, InputStream source, String dest_filename, String host, int port, String user, String password) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(new ByteArrayInputStream(ctx.getResources().getText(R.string.ssh_known_hosts).toString().getBytes()));
        Session session = jsch.getSession(user, host, port == -1 ? 22 : port);
        if (password != null)
            session.setPassword(password);
        session.connect();
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(source, dest_filename);
            channel.disconnect();
        } finally {
            session.disconnect();
        }
    }

    private static void upload(Context ctx, String url_spec) throws URISyntaxException, IOException, SftpException, JSchException {
        URI uri = new URI(url_spec);
        String userinfo = uri.getUserInfo();
        String username = null, password = null;
        if (userinfo != null) {
            int sep = userinfo.indexOf(':');
            if (sep == -1) {
                username = userinfo;
                password = null;
            } else {
                username = userinfo.substring(0, sep);
                password = userinfo.substring(sep + 1);
            }
        }

        long timestamp = new Date().getTime() / 1000L;
        String dest_filename = String.format("%s-%d.sqlite3.gz", Preferences.getInstallID(ctx), timestamp);

        File dbfile = Utils.createTempFile(ctx);
        try {
            Utils.copyFileGzipped(Database.getDataFile(ctx), dbfile);
            InputStream source = new FileInputStream(dbfile);
            try {
                if (uri.getScheme() == "ftp")
                    uploadFtp(ctx, source, dest_filename, uri.getHost(), username, password);
                else
                    uploadSftp(ctx, source, dest_filename, uri.getHost(), uri.getPort(), username, password);
            } finally {
                source.close();
            }
        } finally {
            dbfile.delete();
        }
    }

    public static Set<String> getSupportedProtocols() {
        Set<String> set = new HashSet<>();
        set.add("ftp");
        set.add("sftp");
        return set;
    }

    @NonNull
    @Override
    public Result doWork() {
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
        } catch (Exception e) {
            notifyError(getApplicationContext(), "Cellscanner upload error", e.getMessage());
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, e.getMessage(), getTags().iterator().next());
            return Result.retry();
        } finally {
            // resume recording
            if (RecorderUtils.isRecordingEnabled(ctx))
                RecorderUtils.startService(ctx);
        }
    }

    private static void scheduleWorkRequest(Context ctx, WorkRequest workRequest) {
        unSchedulePeriodDataUpload(ctx);
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
                .Builder(UserDataUploadWorker.class, 15, TimeUnit.MINUTES) // TODO: Make this a useful setting
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
