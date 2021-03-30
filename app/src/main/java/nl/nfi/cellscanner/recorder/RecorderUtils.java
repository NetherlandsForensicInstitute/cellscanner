package nl.nfi.cellscanner.recorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

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

    /**
     * Request the start of recording user data
     * <p>
     * test for the right permissions, if ok, start recording. Otherwise request permissions
     */
    public static void requestStartRecording(final PreferencesActivity ctx) {
        final List<String> missing_permissions = PermissionSupport.getMissingPermissions(ctx);

        if (missing_permissions.isEmpty())
            RecorderUtils.startService(ctx);
        else {
            // the following sentence is required by Google prominent disclosure policy
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setMessage(ctx.getResources().getText(R.string.prominent_disclosure_text))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(ctx, missing_permissions.toArray(new String[]{}), PERMISSION_REQUEST_START_RECORDING);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ctx.prefs.setRecordingEnabled(false);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Check the state of the Recording key.
     * @return State of the Recording key, when True the app should record cell data
     */
    public static boolean isRecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Preferences.PREF_ENABLE, false);
    }

    /**
     * Starts the recording service
     *
     * Staring method is based on the API lvl the app is running on/
     * @param context: Context starting the service
     */
    public static void startService(Context context) {
        // after API-26 the service should be a Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, new Intent(context, LocationRecordingService.class));
        } else {
            context.startService(new Intent(context, LocationRecordingService.class));
        }
    }

    /**
     * Stops the service and sets the recording state to False
     * @param context: Context Stopping the service
     */
    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, LocationRecordingService.class);
        context.stopService(serviceIntent);
    }
}