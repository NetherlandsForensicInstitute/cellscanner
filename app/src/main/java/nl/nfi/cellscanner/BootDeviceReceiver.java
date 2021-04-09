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
        RecorderUtils.applyRecordingPolicy(context);
        UserDataUploadWorker.applyUploadPolicy(context);
    }
}
