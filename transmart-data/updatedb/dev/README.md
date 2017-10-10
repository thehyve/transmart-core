Database update scripts for dev (WIP)
========================================

Overview
--------

## Database structure changes


### Primary keys


## Data Migration

### Variable type in visual attributes

The type of a variable is now stored as visual attributes in the i2b2 tree,
so that the type does not have to be derived from the XML in the metadata blob.

Only `i2b2_secure` is updated, as the `i2b2` table is redundant and is considered deprecated.

`data_migration.sql` takes care of adding the appropriate visual attributes to leaf nodes.
See script's comments for more details.


How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
