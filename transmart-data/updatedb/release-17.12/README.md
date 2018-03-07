Database update scripts for release 17.12
=========================================

Overview
--------

## Database structure changes

### Query subscription

| Script                             | Description                                     |
|------------------------------------|-------------------------------------------------|
| `query_set.sql`          | Adds 'biomart_user'.'query_set table'          |
| `query_set_instance.sql` | Adds 'biomart_user'.'query_set_instance table' |
| `query_set_diff.sql`     | Adds 'biomart_user'.'query_set_diff table'     |



## Data Migration

`data_migration.sql` takes care of adding new columns required for query subscription feature to 'biomart_user'.'query' table.

How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
