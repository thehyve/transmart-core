Database update scripts for release 17.1-HYVE-5.7
========================================

Overview
--------

## Data migration

### Increase the path length for tags to 700.

The path length already was 700 in the `i2b2_secure` table, but was limited
to 400 in the `i2b2_tags` table. This change make the path length consistent
in both at 700 characters.

## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
# For Oracle:
make -C oracle migrate
```
