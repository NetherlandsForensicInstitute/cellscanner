package nl.nfi.cellscanner.collect.cellinfo;

import android.content.ContentValues;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;

import java.util.Locale;

public class CellStatus {
    public final boolean registered;
    public final String radio;
    public final int mcc;
    public final int mnc;
    public final int area;
    public final int cid;
    public final int bsic; // gsm
    public final int arfcn; // gsm
    public final int psc; // umts
    public final int uarfcn; // umts
    public final int pci; // lte

    public static class UnsupportedTypeException extends Exception {
        UnsupportedTypeException(String msg) {
            super(msg);
        }
    }

    public CellStatus(boolean registered, String radio, int mcc, int mnc, int area, int cid, int bsic, int arfcn, int psc, int uarfcn, int pci) {
        this.registered = registered;
        this.radio = radio;
        this.mcc = mcc;
        this.mnc = mnc;
        this.area = area;
        this.cid = cid;
        this.bsic = bsic;
        this.arfcn = arfcn;
        this.psc = psc;
        this.uarfcn = uarfcn;
        this.pci = pci;
    }

    public boolean isValid() {
        // TODO: improve by example https://github.com/zamojski/TowerCollector/tree/master/app/src/main/java/info/zamojski/soft/towercollector/collector/validators
        if (!registered)
            return false;

        if (mcc == 0 || mcc == 0x7fffffff)
            return false;

        return true;
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put("registered", registered ? 1 : 0);
        values.put("radio", radio);
        values.put("mcc", mcc);
        values.put("mnc", mnc);
        values.put("area", area);
        values.put("cid", cid);
        values.put("bsic", bsic);
        values.put("arfcn", arfcn);
        values.put("psc", psc);
        values.put("uarfcn", uarfcn);
        values.put("pci", pci);

        return values;
    }

    public static CellStatus fromCellInfo(CellInfo info) throws UnsupportedTypeException {
        if (info instanceof CellInfoGsm) {
            return fromCellInfoGsm((CellInfoGsm)info);
        } else if (info instanceof CellInfoCdma) {
            throw new UnsupportedTypeException("Unsupported cell info object: "+CellInfoCdma.class.getName());
        } else if (info instanceof CellInfoWcdma) {
            return fromCellInfoWcdma((CellInfoWcdma)info);
        } else if (info instanceof CellInfoLte) {
            return fromCellInfoLte((CellInfoLte) info);
        } else {
            throw new UnsupportedTypeException("Unrecognized cell info object: "+info.getClass().getName());
        }
    }

    private static CellStatus fromCellInfoGsm(CellInfoGsm info) {
        return new CellStatus(
            info.isRegistered(),
            "GSM",
            info.getCellIdentity().getMcc(),
            info.getCellIdentity().getMnc(),
            info.getCellIdentity().getLac(),
            info.getCellIdentity().getCid(),
            info.getCellIdentity().getBsic(),
            info.getCellIdentity().getArfcn(),
            -1, -1,-1
        );
    }

    private static CellStatus fromCellInfoWcdma(CellInfoWcdma info) {
        return new CellStatus(
                info.isRegistered(),
                "UMTS",
                info.getCellIdentity().getMcc(),
                info.getCellIdentity().getMnc(),
                info.getCellIdentity().getLac(),
                info.getCellIdentity().getCid(),
                -1, -1,
                info.getCellIdentity().getPsc(),
                info.getCellIdentity().getUarfcn(),
                -1
        );
    }

    private static CellStatus fromCellInfoLte(CellInfoLte info) {
        return new CellStatus(
                info.isRegistered(),
                "LTE",
                info.getCellIdentity().getMcc(),
                info.getCellIdentity().getMnc(),
                info.getCellIdentity().getTac(),
                info.getCellIdentity().getCi(),
                -1, -1, -1, -1,
                info.getCellIdentity().getPci()
        );
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s%s: %d-%d-%d-%d", registered ? "" : "unregistered: ", radio, mcc, mnc, area, cid);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CellStatus) {
            CellStatus other = (CellStatus) o;

            return registered == other.registered
                    && radio.equals(other.radio)
                    && mcc == other.mcc
                    && mnc == other.mnc
                    && area == other.area
                    && cid == other.cid;
        } else {
            return false;
        }
    }
}
