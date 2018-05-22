Database update scripts for release 17.1-HYVE-4
========================================

Overview
--------

Views

## Database structure changes

### Bitset patient counts

PostgreSQL extension function (`pg_bitcount(bitset)`) to count number of bits in the bitset optimally,
and bunch of views to count number of patients per study and concept.

How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    
Optimizations for oracle have not been implemented.