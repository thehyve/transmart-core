--
-- drop foreign key for data_id and de_subject_rbm_data(id) de_rbm_data_annotation_join
--

set search_path = deapp, pg_catalog;

ALTER TABLE ONLY de_rbm_data_annotation_join DROP CONSTRAINT de_rbm_data_ann_jn_data_id_fk;
