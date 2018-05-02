Database update scripts for release 17.1-HYVE-3
========================================

Overview
--------

The user query table was extended with a metadata column.

## Database structure changes

### Metadata column added to queries

`query_blob.sql` takes care of adding a new `query_blob` column to the `biomart_user.query` table
in order to store additional information about queries.

How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
