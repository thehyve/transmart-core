Database update scripts for release 17.1-HYVE-5.2
========================================

Overview
--------

## Data migration

### Improve performance for patient set view.

Apply `pg_int_to_bit_agg` aggregate to improve performance of bitset operations.

## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
```      

This improvement is not applicable to Oracle databases.
