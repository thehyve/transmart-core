Database update scripts for release 17.1-HYVE-5.8
========================================

Overview
--------

## Data migration

### Add columns to the `i2b2metadata.dimension_description` table.

Add columns:
- `dimension_type` Indicates whether the dimension represents subjects or observation attributes. [`SUBJECT`, `ATTRIBUTE`].
- `sort_index`. Specifies a relative order between dimensions.

Also documentation on the table and default values are added.

### Add index on `result_instance_id` and `patient_num` to the `qt_patient_set_collection` table

This new index improves the speed of querying patient sets.


## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
# For Oracle:
make -C oracle migrate
```      
