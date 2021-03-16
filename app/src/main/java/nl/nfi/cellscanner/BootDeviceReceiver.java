package nl.nfi.cellscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import nl.nfi.cellscanner.recorder.PermissionSupport;
import nl.nfi.cellscanner.recorder.RecorderUtils;

/**
 * Responsible for starting the App after device boot
 *
 * Checks if the application is was left in a recording state before the reboot, if true
 * starts the application after the boot sequence has been completed
 */
public class BootDeviceReceiver extends BroadcastReceiver {

    private static final String TAG = BootDeviceReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        String message = CellScannerApp.TITLE + " on boot action is " + action;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // TODO: @ check if the right application is sending the request
        /*
          Call the start of the application if the boot-up state of the application
          is inRecordingState
         */
        if (RecorderUtils.isRecordingEnabled(context)){
            RecorderUtils.startService(context);
        }

        UserDataUploadWorker.applyUploadPolicy(context);
    }
}
