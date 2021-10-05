package nl.nfi.cellscanner.collect.cellinfo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import nl.nfi.cellscanner.CellscannerApp;
import nl.nfi.cellscanner.PermissionSupport;
import nl.nfi.cellscanner.collect.CollectorFactory;
import nl.nfi.cellscanner.collect.DataCollector;
import nl.nfi.cellscanner.collect.DataReceiver;
import nl.nfi.cellscanner.collect.LocationCollector;

public class TimerCellInfoCollector implements DataCollector {
    // update frequency of network data
    public static int UPDATE_DELAY_MILLIS = 1000;
    public static int UPDATE_RESET_MILLIS = 1000*60;

    public static final String[] PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private final DataReceiver receiver;
    private final TelephonyManager defaultTelephonyManager;
    private final Map<String, CellInfoState> states = new HashMap<String,CellInfoState>();
    private Date last_date = null;

    public TimerCellInfoCollector(DataReceiver receiver) {
        this.receiver = receiver;
        defaultTelephonyManager = (TelephonyManager) receiver.getContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

    private Timer timer = null;

    @Override
    public String[] requiredPermissions(Intent intent) {
        return PERMISSIONS;
    }

    @Override
    public synchronized void resume(Intent intent) {
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    retrieveCellInfo();
                }
            }, UPDATE_DELAY_MILLIS, UPDATE_DELAY_MILLIS);
        }
    }

    @Override
    public synchronized void cleanup() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Retrieve the current CellInfo, update;
     * - database
     * - Service notification
     * - send broadcast to update App
     */
    private void retrieveCellInfo() {
        try {
            if (PermissionSupport.hasPermissions(receiver.getContext(), PERMISSIONS)) {
                if (last_date != null && new Date().getTime() > last_date.getTime() + UPDATE_RESET_MILLIS) {
                    // cell status has expired
                    states.clear();
                }

                last_date = new Date();

                @SuppressLint("MissingPermission") List<CellInfo> cellinfos = defaultTelephonyManager.getAllCellInfo();

                Set<String> seen = new HashSet<String>();
                for (CellInfo cellinfo : cellinfos) {
                    CellStatus status = CellStatus.fromCellInfo(cellinfo);
                    if (status.isValid()) {
                        String name = String.format("%d-%d", status.mcc, status.mnc);

                        if (!states.containsKey(name))
                            states.put(name, new CellInfoState(name, receiver));

                        states.get(name).updateCellStatus(status);
                        seen.add(name);
                    }
                }

                for (Map.Entry<String, CellInfoState> item : states.entrySet()) {
                    if (!seen.contains(item.getKey())) {
                        item.getValue().updateCellStatus(null);
                    }
                }
            } else {
                Log.w("cellscanner", "insufficient permissions to get cell info");
                cleanup();
            }
        } catch (Throwable e) {
            CellscannerApp.getDatabase().storeMessage(e);
        }
    }
}
