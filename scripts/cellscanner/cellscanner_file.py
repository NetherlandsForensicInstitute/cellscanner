from contextlib import closing
import datetime

import pytz


LOCATION_COLUMNS = ['timestamp', 'provider', 'latitude', 'longitude', 'accuracy', 'altitude', 'altitude_acc', 'speed', 'speed_acc',
                    'bearing_deg', 'bearing_deg_acc']

CELL_COLUMNS = ['date_start', 'date_end', 'subscription', 'registered', 'radio', 'mcc', 'mnc', 'area', 'cid', 'arfcn', 'psc', 'uarfcn', 'pci']


class CellscannerFile:
    def __init__(self, con):
        self._con = con

    def get_install_id(self):
        with closing(self._con.cursor()) as cur:
            cur.execute("SELECT value FROM meta WHERE entry = 'install_id'")
            row = cur.fetchone()
            return row[0]

    def get_locationinfo(self):
        with closing(self._con.cursor()) as cur:
            cur.execute(f"SELECT {','.join(col for col in LOCATION_COLUMNS)} FROM locationinfo")
            for row in cur.fetchall():
                row = list(row)
                row[0] = datetime.datetime.utcfromtimestamp(int(row[0])/1000).astimezone(pytz.utc)
                yield row

    def get_cellinfo(self):
        with closing(self._con.cursor()) as cur:
            cur.execute(f"SELECT {','.join(col for col in CELL_COLUMNS)} FROM cellinfo")
            for row in cur.fetchall():
                row = list(row)
                for i in [0, 1]:
                    row[i] = datetime.datetime.utcfromtimestamp(int(row[i])/1000).astimezone(pytz.utc)
                yield row

    def get_joint_measurements(self, timediff_secs: int):
        LOCATION_COLUMNS = ['latitude', 'longitude', 'accuracy', 'altitude', 'altitude_acc', 'speed', 'speed_acc', 'bearing_deg', 'bearing_deg_acc' ]
        CELL_COLUMNS = ['subscription', 'radio', 'mcc', 'mnc', 'lac', 'cid', 'area', 'arfcn', 'psc', 'uarfcn', 'pci']
        with closing(self._con.cursor()) as cur:
            cur.execute(f"""
                SELECT locationinfo.timestamp,
                    {','.join(colname for colname in LOCATION_COLUMNS)},
                    {','.join(colname for colname in CELL_COLUMNS)}
                FROM locationinfo
                    JOIN cellinfo ON locationinfo.timestamp - {timediff_secs} >= cellinfo.date_start AND locationinfo.timestamp - {timediff_secs} < cellinfo.date_end
            """)
