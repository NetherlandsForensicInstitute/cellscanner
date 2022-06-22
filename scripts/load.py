#!/usr/bin/env python3

import argparse
import os
import sqlite3

import confidence

from cellscanner.postgres import pgconnect, drop_schema, create_schema
import cellscanner.cellscanner_file
import cellscanner.database


if __name__ == '__main__':
    cfg = confidence.load_name('cellscanner', 'local')

    parser = argparse.ArgumentParser(description='Loads cellscanner sqlite data into a postgres database')
    parser.add_argument('--drop-schema', action='store_true', help='drop schema before doing anything else')
    parser.add_argument('--schema', help='load into schema', default=cfg.database.schema)
    parser.add_argument('--tag', help='assign tag to device')
    parser.add_argument('-v', help='increases verbosity', action='count', default=0)
    parser.add_argument('-q', help='decreases verbosity', action='count', default=0)
    parser.add_argument('files', metavar='FILE', nargs='+')
    args = parser.parse_args()

    with pgconnect(credentials=cfg.database.credentials) as con:
        if args.drop_schema:
            drop_schema(con, args.schema)
        create_schema(con, args.schema)

    with pgconnect(credentials=cfg.database.credentials, schema=args.schema, use_wrapper=False) as target_connection:
        db = cellscanner.database.CellscannerDatabase(target_connection)
        db.create_tables()
        for filename in args.files:
            assert os.path.exists(filename), f'file not found: {filename}'
            with sqlite3.connect(filename) as source_connection:
                csfile = cellscanner.cellscanner_file.CellscannerFile(source_connection)
                install_id = csfile.get_install_id()
                device_handle = db.add_device(install_id, args.tag)
                db.add_locationinfo(device_handle, csfile.get_locationinfo())
                db.add_cellinfo(device_handle, csfile.get_cellinfo())
