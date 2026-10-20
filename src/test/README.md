# MySQL integration test

The regular test suite uses an in-memory HSQLDB database. The optional
`MySqlPersistenceTest` runs only when `AFTER_MYSQL_TEST_URL` is set to exactly
`jdbc:mysql://localhost:3306/after_test` or
`jdbc:mysql://127.0.0.1:3306/after_test`.

Create a separate, disposable `after_test` database and a dedicated user before
running it. **The test uses `create-drop` and destroys tables in that database.**
Never point it at an application database or a database containing valuable data.

Set `AFTER_MYSQL_TEST_URL`, `AFTER_MYSQL_TEST_USERNAME`, and
`AFTER_MYSQL_TEST_PASSWORD` in the environment, then run:

```sh
./mvnw -Dtest=MySqlPersistenceTest test
```

The database and its credentials are not created or managed by this test.
