-- Add columns to the query table for subscription feature
alter table biomart_user.query
  add column query_blob text;

COMMENT ON COLUMN biomart_user.query.query_blob IS 'Additional information with user preferences e.g. data table state';

-- subject bitset counts resources
\i ../../../ddl/postgres/biomart_user/views/patient_num_boundaries.sql
\i ../../../ddl/postgres/biomart_user/materialized_views/study_concept_bitset.sql
\i ../../../ddl/postgres/biomart_user/views/patient_set_bitset.sql
\i ../../../ddl/postgres/biomart_user/views/study_concept_patient_set_bitset.sql