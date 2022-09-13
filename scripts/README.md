Step 0: prepare your environment
--------------------------------

Install dependencies if not already done: python and postgres.

Create a postgres database and user.

Step 1: create a virtual environment
------------------------------------

```sh
virtualenv -p python3 venv
source venv/bin/activate
pip install -r requirements.txt
```

Step 2: create a configuration
------------------------------

Create a file named `local.yaml`. You may override any default settings in
`cellscanner.yaml`. In particular, enter the postgres password.

```yaml
database.credentials.password: SECRET
```

Step 3: load the Sqlite3 databases into postgres
------------------------------------------------

```sh
./load.py cellscanner.sqlite3
```
