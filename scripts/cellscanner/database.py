from typing import Iterable, Tuple

from .cellscanner_file import LOCATION_COLUMNS, CELL_COLUMNS


class CellscannerDatabase:
    def __init__(self, con):
        self._con = con

    def create_tables(self):
        with self._con.cursor() as cur:
            cur.execute("""
                CREATE TABLE IF NOT EXISTS device (
                    id SERIAL NOT NULL PRIMARY KEY,
                    install_id VARCHAR(255) UNIQUE NOT NULL,
                    tag VARCHAR(100) NULL
                )""")

            cur.execute("""
                CREATE TABLE IF NOT EXISTS message (
                    device_id INT NOT NULL REFERENCES device(id),
                    date TIMESTAMP WITH TIME ZONE NOT NULL,
                    message VARCHAR(250) NOT NULL
                )""")
            cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS message_date ON message(device_id, date)")

            cur.execute("""
                CREATE TABLE IF NOT EXISTS ip_traffic (
                    device_id INT NOT NULL REFERENCES device(id),
                    date_start TIMESTAMP WITH TIME ZONE NOT NULL,
                    date_end TIMESTAMP WITH TIME ZONE,
                    bytes_read INT NOT NULL
                )""")
            cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS ip_traffic_date_start ON ip_traffic(device_id, date_start)")

            cur.execute("""
                CREATE TABLE IF NOT EXISTS locationinfo (
                    device_id INT NOT NULL REFERENCES device(id),
                    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                    provider VARCHAR(200),
                    latitude FLOAT NOT NULL,
                    longitude FLOAT NOT NULL,
                    accuracy INT,
                    altitude INT,
                    altitude_acc INT,
                    speed INT,
                    speed_acc INT,
                    bearing_deg INT,
                    bearing_deg_acc INT
                )""")
            cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS locationinfo_timestamp ON locationinfo(device_id, timestamp)")

            cur.execute("""
                CREATE TABLE IF NOT EXISTS cellinfo (
                    device_id INT NOT NULL REFERENCES device(id),
                    subscription VARCHAR(20) NOT NULL,
                    date_start TIMESTAMP WITH TIME ZONE NOT NULL,
                    date_end TIMESTAMP WITH TIME ZONE NOT NULL,
                    registered INT NOT NULL,
                    radio VARCHAR(10) NOT NULL,
                    mcc INT NOT NULL,
                    mnc INT NOT NULL,
                    area INT NOT NULL,
                    cid INT NOT NULL,
                    bsic INT,
                    arfcn INT,
                    psc INT,
                    uarfcn INT,
                    pci INT
                )""")
            cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS cellinfo_subscription_date_start ON cellinfo(device_id, subscription, date_start)")

            cur.execute("""
                CREATE TABLE IF NOT EXISTS call_state (
                    device_id INT NOT NULL REFERENCES device(id),
                    date TIMESTAMP WITH TIME ZONE NOT NULL,
                    state VARCHAR(20) NOT NULL
                )""")
            cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS call_state_date ON call_state(device_id, date)")

    def add_device(self, install_id, tag, exist_ok=True):
        if exist_ok:
            with self._con.cursor() as cur:
                cur.execute("SELECT id, tag FROM device WHERE install_id = %s", (install_id,))
                row = cur.fetchone()
                if row:
                    assert row[1] == tag, f"tag ({tag}) of device {install_id} does not match previously assign tag ({row[1]})"
                    return row[0]

        with self._con.cursor() as cur:
            cur.execute("""INSERT INTO device(install_id, tag) VALUES(%s, %s) RETURNING id""", (install_id, tag))
            return cur.fetchone()[0]

    def add_locationinfo(self, device_id, records: Iterable[Tuple]):
        with self._con.cursor() as cur:
            for record in records:
                cur.execute(f"""
                    INSERT INTO locationinfo(device_id, {",".join(col for col in LOCATION_COLUMNS)})
                    VALUES(%s, {",".join("%s" for col in LOCATION_COLUMNS)})
                    ON CONFLICT (device_id, timestamp) DO NOTHING
                """, [device_id] + list(record))

    def add_cellinfo(self, device_id, records: Iterable[Tuple]):
        with self._con.cursor() as cur:
            for record in records:
                cur.execute(f"""
                    INSERT INTO cellinfo(device_id, {",".join(col for col in CELL_COLUMNS)})
                    VALUES(%s, {",".join("%s" for col in CELL_COLUMNS)})
                    ON CONFLICT(device_id, subscription, date_start) DO NOTHING
                """, [device_id] + list(record))
