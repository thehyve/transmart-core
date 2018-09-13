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
| `make oracle_clean_all_data` | Deletes *all rows* for the database tables data files of which could be found in the folder. |

## Notes

There is no transformation step involved, only loading. There are tsv files in this folder that corresponds to the database tables.
The location of these files matters and should look like this: `{schema}/{table}.tsv`.
You could influence on the order of loading the tables by declaring dependencies between tables in the `parse_and_reorder.groovy` script.
The tsv files do not have headers. The order of their columns are important and matches the columns in the database table.
