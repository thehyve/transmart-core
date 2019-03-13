Database update scripts for release 17.1-HYVE-5.8
========================================

Overview
--------

## Data migration

### Add columns to the `i2b2metadata.dimension_description` table.

Add columns:
- `dimension_type` Indicates whether the dimension represents subjects or observation attributes. [`SUBJECT`, `ATTRIBUTE`].
- `sort_index`. Specifies a relative order between dimensions.

## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
# For Oracle:
make -C oracle migrate
```      
