Database update scripts for release 17.1-HYVE-4
========================================

Overview
--------

Views

## Database structure changes

### Bitset patient counts

PostgreSQL extension function (`pg_bitcount(bitset)`) to count number of bits in the bitset optimally,
and bunch of views to count number of patients per study and concept.

## Data migrations

### Get rid of negative patient identifiers in the test data

Given that we used to start patient identifiers with billion smth. negative patient identifiers suggest huge bitset strings.
That cases the bitset patient counts to become unreasonably slow.
`fix_neg_patient_nums.sql` fixes this issue by giving new, positive ids to patients with negative ids.

**Note:** ⚠️
The script fixes patient numbers in the tables used by the test studies (`test_data` folder). It does not scan other tables.

    make -C postgres fix_test_data

How to apply the migration scripts
----------------------------------

Given that transmart-data is configured correctly, you could run one of the following make commands:
    
    make -C postgres migrate
    
Optimizations for oracle have not been implemented.