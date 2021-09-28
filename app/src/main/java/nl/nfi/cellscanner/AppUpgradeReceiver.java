package nl.nfi.cellscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import nl.nfi.cellscanner.collect.RecorderUtils;

/**
 * Responsible for starting the App after device boot
 *
 * Checks if the application is was left in a recording state before the reboot, if true
 * starts the application after the boot sequence has been completed
 */
public class AppUpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        RecorderUtils.applyRecordingPolicy(context);
        UserDataUploadWorker.applyUploadPolicy(context);
    }
}
