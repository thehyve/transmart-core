Database update scripts for release 17.1-HYVE-6.0
========================================

Overview
--------

## Data migration

### Change the primary key of the `i2b2metadata.visit_dimension` table.

- Use a single column `encounter_num` instead of the combination of
  `encounter_num` and `patient_num`.
- Add foreign keys to the `visit_dimension` and `encounter_mapping` tables
  for references to other tables.


## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
# For Oracle:
make -C oracle migrate
```      
