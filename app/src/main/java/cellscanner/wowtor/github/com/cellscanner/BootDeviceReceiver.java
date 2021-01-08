package cellscanner.wowtor.github.com.cellscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import cellscanner.wowtor.github.com.cellscanner.recorder.Recorder;

/**
 * Responsible for starting the App after device boot
 *
 * Checks if the application is was left in a recording state before the reboot, if true
 * starts the application after the boot sequence has been completed
 */
public class BootDeviceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        String message = App.TITLE + " on boot action is " + action;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // TODO: @ check if the right application is sending the request
        if (Recorder.inRecordingState(context)){
            // TODO: Move this logic to the Recorder Class
            // The recorder class should decide to run the service as Android 8 + or not
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Recorder.startService(context);
            } else {
                LocationService.start(context);
            }
        }
    }
}
