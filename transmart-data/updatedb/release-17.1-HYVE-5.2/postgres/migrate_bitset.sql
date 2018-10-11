drop view biomart_user.study_patient_set_bitset;
drop view biomart_user.concept_patient_set_bitset;
drop view biomart_user.study_concept_patient_set_bitset;
drop materialized view biomart_user.study_concept_bitset;
drop view biomart_user.patient_set_bitset;
drop view biomart_user.patient_num_boundaries;

drop extension if exists pg_bitcount;
create extension pg_bitcount version '0.0.3';

\i ../../../ddl/postgres/biomart_user/views/patient_num_boundaries.sql
\i ../../../ddl/postgres/biomart_user/views/patient_set_bitset.sql
\i ../../../ddl/postgres/biomart_user/materialized_views/study_concept_bitset.sql
\i ../../../ddl/postgres/biomart_user/views/study_patient_set_bitset.sql
\i ../../../ddl/postgres/biomart_user/views/concept_patient_set_bitset.sql
\i ../../../ddl/postgres/biomart_user/views/study_concept_patient_set_bitset.sql
