Database update scripts for release 17.1-HYVE-5
========================================

Overview
--------

## Data Migration

### Migrate access levels

The access levels in the `searchapp.search_sec_access_level` table have been renamed
to better reflect their meaning in TranSMART.
The `OWN` and `EXPORT` levels have been merged and renamed to `MEASUREMENTS`. This
reflects that with these access levels, users could access full observations data.
The `VIEW` access level only gave access to summary statistics and counts and is renamed
to `SUMMARY`.
A new access level, `COUNTS_WITH_THRESHOLD` is introduced, which can be used to indicate
that a user has access to counts, but only if the counts are below a configured threshold.
This does not provide [_k_-anonymity], but does reduce the precision of the counts,
which makes it harder to derive observation data from aggregates.

| Previous name | New name                |
| ------------- | ----------------------- |
| `OWN`         | `MEASUREMENTS`          |
| `EXPORT`      | `MEASUREMENTS`          |
| `VIEW`        | `SUMMARY`               |
|               | `COUNTS_WITH_THRESHOLD` |

### Fix bit-set for empty patient set

`patient_set_bitset` view returned empty data set (0 rows) when patient set had no rows.
It caused issues with this bitset/constraint being ignored.
The fix is to return zero bit-set (bit string with all zeros) in this case.

## How to apply all changes

Given that transmart-data is configured correctly, you can apply the changes using one of the following make commands:

```bash
# For PostgreSQL:
make -C postgres migrate
# For Oracle:
make -C oracle migrate
```      

[_k_-anonymity]: https://en.wikipedia.org/wiki/K-anonymity
