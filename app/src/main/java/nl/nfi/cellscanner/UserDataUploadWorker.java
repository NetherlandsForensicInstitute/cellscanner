package nl.nfi.cellscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;
import java.util.Date;

import static nl.nfi.cellscanner.CellScannerApp.getDatabase;
import static nl.nfi.cellscanner.PreferencesActivity.getInstallID;

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
        FileInputStream fileInputStream;
        FTPClient con = new FTPClient();

        long timestamp  = getTimeStamp();

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

                String serverSideFileName = getFileName(
                        getInstallID(getApplicationContext()),
                        timestamp
                );

                // Upload the file
                boolean result = con.storeFile(serverSideFileName, fileInputStream);
                fileInputStream.close();

                if (result) {
                    // SIGNAL our success
                    Log.i(TAG, "upload result: succeeded");
                    ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, true, "success", ExportResultRepository.AUTO);
                    db.dropDataUntil(timestamp);

                } else {
                    // SIGNAL their failure
                    Log.i(TAG, "upload result: Failed");
                    ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, "unknown failure", ExportResultRepository.AUTO);

                }

                // disconnect from the server
                con.logout();
                con.disconnect();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ExportResultRepository.storeExportResult(getApplicationContext(), timestamp, false, e.getMessage(), ExportResultRepository.AUTO);

        }



        return Result.success();
    }


    /**
     * Method returns how many milliseconds have passed since January 1, 1970, 00:00:00 GMT
     * @return UTC timestamp
     */
    private long getTimeStamp() {
        return new Date().getTime();
    }

    @SuppressLint("DefaultLocale")
    private String getFileName(String aDeviceId, long aTimeStamp) {
        return String.format("%s-%d.sqlite", aDeviceId, aTimeStamp / 1000L);
    }
}
