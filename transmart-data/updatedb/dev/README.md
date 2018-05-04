Database update scripts for dev (WIP)
========================================

Overview
--------

## Database structure changes


### Primary keys


## Data Migration


How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
