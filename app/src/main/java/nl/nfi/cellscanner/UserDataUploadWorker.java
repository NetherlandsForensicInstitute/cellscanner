package nl.nfi.cellscanner;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;


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
            con.connect(HOSTNAME);
            if (con.login(USERNAME, MYPASS))
            {
                con.enterLocalPassiveMode(); // important!
                con.setFileType(FTP.BINARY_FILE_TYPE);

                // get the file and send it. A
                fileInputStream = new FileInputStream(Database.getDataFile(getApplicationContext()));
                boolean result = con.storeFile(getFileName("deviceID", ""), fileInputStream);
                fileInputStream.close();

                if (result) {
                    Log.i(TAG, "upload result: succeeded");

                    /*when file has been uploaded, the old data can be flushed*/

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


    private String getFileName(String deviceId, String timeStamp) {
        return String.format("s{0}.sqlite", deviceId);
    }
}
