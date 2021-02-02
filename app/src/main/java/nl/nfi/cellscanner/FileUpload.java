package nl.nfi.cellscanner;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
Check if we can remove the service part... now it is only in
use to get the application context, and so the file name
 */
public class FileUpload extends Service {

    private static final String TAG = FileUpload.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Upload datafile to FTP server.
     */
    private class FTPUpload implements Runnable {

        private static final String HOSTNAME = "192.168.2.6";
        private static final String USERNAME = "myuser";
        private static final String MYPASS = "mypass";

        String deviceID = "";
        String pathToDatabase = "";


        public FTPUpload(String deviceID, String pathToDataBase) {
            this.deviceID = deviceID;
            this.pathToDatabase = pathToDataBase;
        }


        @Override
        public void run() {

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
                    boolean result = con.storeFile(getFileName(deviceID, ""), fileInputStream);
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

        }
    }

    /**
     * Construct the name of the file, as it should be stored on the external device
     */
    private String getFileName(String deviceId, String timeStamp) {
        return String.format("${0}.sqlite", deviceId);
    }

    /**
     * Pickup the database and send it to the FTP
     */
    public void uploadApplicationDatabase(){
        Runnable action = new FTPUpload("123", "omg");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(action);
    }
}
