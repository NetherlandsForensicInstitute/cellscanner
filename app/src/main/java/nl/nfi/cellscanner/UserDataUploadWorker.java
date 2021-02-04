package nl.nfi.cellscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;
import java.util.Date;

import nl.nfi.cellscanner.recorder.RecorderUtils;
import static nl.nfi.cellscanner.CellScannerApp.getDatabase;

public class UserDataUploadWorker extends Worker {
    public static final String TAG = UserDataUploadWorker.class.getSimpleName();

    private static final String HOSTNAME = "192.168.2.29";
    private static final String USERNAME = "myuser";
    private static final String MYPASS = "mypass";


    public UserDataUploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }


    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Start upload of data file");

        FileInputStream fileInputStream;
        FTPClient con = new FTPClient();


        try
        {
            Database db = getDatabase();

            con.connect(HOSTNAME);
            if (con.login(USERNAME, MYPASS))
            {
                con.enterLocalPassiveMode(); // important!
                con.setFileType(FTP.BINARY_FILE_TYPE);

                // get the file and send it.
                fileInputStream = new FileInputStream(Database.getDataFile(getApplicationContext()));
                long timestamp  = getTimeStamp();

                String serverSideFileName = getFileName(
                        RecorderUtils.getPrefInstallId(getApplicationContext()),
                        timestamp
                );

                // Upload the file
                boolean result = con.storeFile(serverSideFileName, fileInputStream);
                fileInputStream.close();

                if (result) {
                    Log.i(TAG, "upload result: succeeded");

                    db.dropDataUntil(timestamp);

                } else {
                    Log.i(TAG, "upload result: Failed");
                }

                // disconnect from the server
                con.logout();
                con.disconnect();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }



        return Result.success();
    }


    private long getTimeStamp() {
        return new Date().getTime();
    }

    @SuppressLint("DefaultLocale")
    private String getFileName(String aDeviceId, long aTimeStamp) {
        return String.format("%s-%d.sqlite", aDeviceId, aTimeStamp / 1000L);
    }
}
