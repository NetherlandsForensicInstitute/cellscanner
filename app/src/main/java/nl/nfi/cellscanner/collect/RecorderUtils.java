package nl.nfi.cellscanner.collect;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.Preferences;
import nl.nfi.cellscanner.PreferencesActivity;
import nl.nfi.cellscanner.R;


/**
 * Responsible for controlling the Reordering service
 *
 * Class gives access to the state of the recording service and allows for the user to start and
 * stop the service
 */
public class RecorderUtils {
    public static final int PERMISSION_REQUEST_START_RECORDING = 1;
    public final static int PERMISSION_REQUEST_PHONE_STATE = 2;

    public static void applyRecordingPolicy(Context context) {
        applyRecordingPolicy(context, null);
    }

    public static void applyRecordingPolicy(Context context, Intent extra) {
        if (Preferences.isRecordingEnabled(context, extra)) {
            if (context instanceof PreferencesActivity)
                requestStartService((PreferencesActivity)context, extra);
            else
                startService(context, extra);
        } else {
            stopService(context);
        }
    }

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    private static void requestStartService(final PreferencesActivity ctx, Intent extra) {
        final List<String> missing_permissions = PermissionSupport.getMissingPermissions(ctx, extra);

        if (missing_permissions.isEmpty())
            RecorderUtils.startService(ctx, extra);
        else {
            if (missing_permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION) || missing_permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // the following sentence is required by Google prominent disclosure policy
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setMessage(ctx.getResources().getText(R.string.prominent_disclosure_text))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(ctx, missing_permissions.toArray(new String[]{}), PERMISSION_REQUEST_START_RECORDING);
                            }
                        })
                        .setNegativeButton(android.R.string.no, (dialog, which) -> ctx.prefs.setRecordingEnabled(false));
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                ActivityCompat.requestPermissions(ctx, missing_permissions.toArray(new String[]{}), PERMISSION_REQUEST_START_RECORDING);
            }
        }
    }

    /**
     * Starts the recording service
     *
     * Staring method is based on the API lvl the app is running on/
     * @param context: Context starting the service
     */
    private static void startService(Context context, Intent extra) {
        Intent intent = new Intent(context, RecordingService.class);
        if (extra != null)
            intent.putExtras(extra);

        // after API-26 the service should be a Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stops the service and sets the recording state to False
     * @param context: Context Stopping the service
     */
    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, RecordingService.class);
        context.stopService(serviceIntent);
    }
}