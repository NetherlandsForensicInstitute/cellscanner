import logging
from typing import Optional, Mapping

import psycopg2
try:
    from sqlalchemy import create_engine
    from sqlalchemy.sql.ddl import CreateSchema
except:
    pass

LOG = logging.getLogger(__name__)


def pgconnect(credentials: Mapping, schema: Optional[str] = None, use_wrapper: bool = True):
    credentials = dict(credentials.items())
    if schema is not None:
        credentials['options'] = '-c search_path={},public'.format(schema)
    con = psycopg2.connect(**credentials)

    if use_wrapper:
        con = Connection(con)

    return con


class Cursor:
    def __init__(self, con, commit_on_close, **kw):
        self.connection = con
        self.commit_on_close = commit_on_close
        self._cur = self.connection._con.cursor(**kw)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.close()

    def execute(self, q, args=None):
        #LOG.debug('query: {q}; args: {args}'.format(q=q, args=args))
        try:
            self._cur.execute(q, args)
        except Exception as e:
            LOG.warning('query failed: {q}; args: {args}; error: {e}'.format(q=q, args=args, e=e))
            raise

    def __getattr__(self, name):
        return eval('self._cur.%s' % name)

    def close(self):
        if self.commit_on_close:
            LOG.debug('commit changes to database')
            self.connection.commit()
        self._cur.close()
        self._cur = None


class Connection:
    def __init__(self, con, autocommit=True):
        self._con = con
        self._autocommit = autocommit
        con.set_client_encoding('UTF8')

    def cursor(self, autocommit=None, **kw):
        return Cursor(self, autocommit if autocommit is not None else self._autocommit, **kw)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.close()

    def commit(self):
        self._con.commit()

    def close(self):
        if self._autocommit:
            self.commit()
        self._con.close()
        self._con = None

    def create_engine(self):
        return create_engine('postgresql://', creator=lambda: self._con)


def drop_schema(con, schema):
    """
    Create or delete a schema in the db connected to by con

    :param con: database connection
    :param schema: schema name
    """
    with con.cursor() as cur:
        cur.execute('DROP SCHEMA IF EXISTS %s CASCADE' % schema)


def create_schema(con, schema):
    """
    Create or delete a schema in the db connected to by con

    :param con: database connection
    :param schema: schema name
    """
    with con.cursor() as cur:
        cur.execute('CREATE SCHEMA IF NOT EXISTS %s' % schema)
