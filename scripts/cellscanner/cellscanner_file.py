from contextlib import closing
import datetime

import pytz


LOCATION_COLUMNS = ['timestamp', 'provider', 'latitude', 'longitude', 'accuracy', 'altitude', 'altitude_acc', 'speed', 'speed_acc',
                    'bearing_deg', 'bearing_deg_acc']

CELL_COLUMNS = ['date_start', 'date_end', 'subscription', 'registered', 'radio', 'mcc', 'mnc', 'area', 'cid', 'arfcn', 'psc', 'uarfcn', 'pci']


class CellscannerFile:
    def __init__(self, con):
        self._con = con

    def _get_meta_value(self, key):
        with closing(self._con.cursor()) as cur:
            cur.execute("SELECT value FROM meta WHERE entry = ?", (key,))
            row = cur.fetchone()
            return row[0]

    def get_version_code(self):
        return int(self._get_meta_value('version_code'))

    def get_install_id(self):
        return self._get_meta_value('install_id')

    def get_locationinfo(self):
        version_code = self.get_version_code()
        if version_code <= 26:
            location_columns = ['timestamp', 'provider', 'latitude', 'longitude', 'accuracy', 'altitude', 'speed']
        else:
            location_columns = LOCATION_COLUMNS

        with closing(self._con.cursor()) as cur:
            cur.execute(f"SELECT {','.join(col for col in location_columns)} FROM locationinfo")
            for row in cur.fetchall():
                row = list(row)
                row[0] = datetime.datetime.utcfromtimestamp(int(row[0])/1000).astimezone(pytz.utc)
                yield dict(zip(location_columns, row))

    def get_cellinfo(self):
        version_code = self.get_version_code()
        if version_code <= 26:
            cell_columns = ['date_start', 'date_end', 'registered', 'radio', 'mcc', 'mnc', 'area', 'cid', 'arfcn', 'psc', 'uarfcn', 'pci']
        else:
            cell_columns = CELL_COLUMNS

        with closing(self._con.cursor()) as cur:
            cur.execute(f"SELECT {','.join(col for col in cell_columns)} FROM cellinfo")
            for row in cur.fetchall():
                row = list(row)
                for i in [0, 1]:
                    row[i] = datetime.datetime.utcfromtimestamp(int(row[i])/1000).astimezone(pytz.utc)
                record = dict(zip(cell_columns, row))
                if 'subscription' not in record:
                    record['subscription'] = f"{record['mcc']}-{record['mnc']}"
                yield record

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
