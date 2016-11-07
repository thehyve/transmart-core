# Test data upload

## Postgres

| Command | Description |
---------|-------------------
| `make postgres_test_data` | Uploads all tsv files found in this folder to the corresponding tables. |
| `make postgres_clean_all_data` | Deletes *all rows* for the database tables data files of which could be found in the folder. |

## Oracle

| Command | Description |
---------|-------------------
| `make oracle_test_data` | Uploads all tsv files found in this folder to the corresponding tables. |
