package nl.nfi.cellscanner.collect.callstate;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;

import java.util.Date;

public class CallStateDatabase {
    private final SQLiteDatabase db;

    public CallStateDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    public void storeCallState(int state) {
        String state_name;
        if (state == TelephonyManager.CALL_STATE_IDLE)
            state_name = "idle";
        else if (state == TelephonyManager.CALL_STATE_RINGING)
            state_name = "ringing";
        else if (state == TelephonyManager.CALL_STATE_OFFHOOK)
            state_name = "offhook";
        else
            state_name = "invalid";

        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("state", state_name);

        db.insert("call_state", null, values);
    }
}
