package cellscanner.wowtor.github.com.cellscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootDeviceReceiver extends BroadcastReceiver {

    private static final String TAG_BOOT_BROADCAST_RECEIVER = "BOOT_BROADCAST_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        String message = App.TITLE + " on boot action is " + action;

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        LocationService.start(context);
    }
}
