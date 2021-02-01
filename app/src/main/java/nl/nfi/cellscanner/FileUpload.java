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

public class FileUpload extends Service {

    private static final String TAG = FileUpload.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class FTPUploadFile implements Runnable {

        public static final String HOSTNAME = "192.168.2.6";
        public static final String USERNAME = "myuser";
        public static final String MYPASS = "mypass";

        @Override
        public void run() {

            FTPClient con = null;

            try
            {
                con = new FTPClient();
                con.connect(HOSTNAME);
                if (con.login(USERNAME, MYPASS))
                {
                    con.enterLocalPassiveMode(); // important!
                    con.setFileType(FTP.BINARY_FILE_TYPE);
                    FileInputStream in = new FileInputStream(Database.getDataFile(getApplicationContext()));
                    boolean result = con.storeFile("/data.base", in);
                    in.close();
                    if (result) {
                        Log.i(TAG, "upload result: succeeded");
                    } else {
                        Log.i(TAG, "upload result: Failed");
                    }

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

    public void uploadFile(){
        Runnable action = new FTPUploadFile();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(action);
    }
}
