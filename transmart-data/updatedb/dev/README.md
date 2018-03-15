Database update scripts for dev (WIP)
========================================

Overview
--------

## Database structure changes


### Primary keys


## Data Migration

`data_migration.sql` takes care of adding a new 'query_blob' column to 'biomart_user'.'query' table in order to store additional information about query.

How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
