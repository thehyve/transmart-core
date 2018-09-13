Database update scripts for release 17.1-HYVE-2
===============================================

Overview
--------

Tables to store subjects relations (e.g. family) were added.

## Database structure changes

### Pedigree relation tables

| Script                             | Description                                     |
|------------------------------------|-------------------------------------------------|
| `i2b2demodata.relation_type.sql`          | Dictionary of relations. e.g. "parent of" relation. |
| `i2b2demodata.relation.sql` | Represents relationships between subjects. e.g. pedigree. |

How to apply all changes
------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    # For PostgreSQL:
    make -C postgres migrate
    # For Oracle:
    make -C oracle migrate
    
