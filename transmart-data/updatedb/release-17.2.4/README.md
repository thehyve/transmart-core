Database updates for release 17.2.4
========================================

Overview
--------

## Data migration

### Update the `i2b2demodata.async_job_seq` sequence

Starting from 17.2.4, the `i2b2demodata.async_job_seq` sequence is used to generate
identifiers for the `async_job` table instead of `search_app.hibernate_sequence`.
The `i2b2demodata.async_job_seq` sequence may need to be updated to prevent identifier
collisions.

```sql
# Fetch highest sequence number of `i2b2demodata.async_job_seq` and `searchapp.hibernate_sequence`
select max(count::int) from (select nextval('i2b2demodata.async_job_seq') as count union select nextval('searchapp.hibernate_sequence') as count) as counts;

# Update the start value of the `async_job_seq` sequence. Use the result of the previous query
# instead of `<VALUE>`
alter sequence i2b2demodata.async_job_seq start with <VALUE>;
```
