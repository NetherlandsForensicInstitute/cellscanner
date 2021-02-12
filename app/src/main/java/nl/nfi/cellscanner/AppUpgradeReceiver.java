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
public class AppUpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /*
          Call the start of the application if the boot-up state of the application
          is inRecordingState
         */
        if (RecorderUtils.inRecordingState(context)){
            if (PermissionSupport.hasAccessCourseLocationPermission(context)) RecorderUtils.startService(context);
            else RecorderUtils.setRecordingState(context, false);
        }
    }
}
