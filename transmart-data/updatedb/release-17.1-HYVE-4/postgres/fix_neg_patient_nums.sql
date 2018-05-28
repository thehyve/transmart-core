BEGIN;
DO $$
DECLARE
min_neg_patient_num numeric;
min_pos_patient_num numeric;
max_pos_patient_num numeric;
BEGIN
	select min(patient_num) into min_neg_patient_num from i2b2demodata.patient_dimension where patient_num < 0;

	IF min_neg_patient_num IS NULL THEN
		RAISE NOTICE 'No negative patient ids found. Nothing to fix.';
	ELSE
		select min(patient_num) into min_pos_patient_num from i2b2demodata.patient_dimension where patient_num >= 0;
		select max(patient_num) into max_pos_patient_num from i2b2demodata.patient_dimension where patient_num >= 0;
		alter table i2b2demodata.relation alter constraint left_subject_id_fk deferrable initially immediate;
		alter table i2b2demodata.relation alter constraint right_subject_id_fk deferrable initially immediate;
		set constraints i2b2demodata.left_subject_id_fk deferred;
		set constraints i2b2demodata.right_subject_id_fk deferred;
		IF min_pos_patient_num IS NULL THEN
			RAISE NOTICE 'No patients with positive identifiers were found. Using the patient id sequence instead.';
			select nextval('i2b2demodata.SEQ_PATIENT_NUM') into min_pos_patient_num;
			max_pos_patient_num := min_pos_patient_num;
		END IF;
		IF min_pos_patient_num > ABS(min_neg_patient_num) THEN
			update i2b2demodata.patient_dimension set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update i2b2demodata.patient_mapping set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update i2b2demodata.patient_trial set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update i2b2demodata.visit_dimension set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update i2b2demodata.observation_fact set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update i2b2demodata.relation set left_subject_id = min_pos_patient_num + left_subject_id where left_subject_id < 0;
			update i2b2demodata.relation set right_subject_id = min_pos_patient_num + right_subject_id where right_subject_id < 0;
			update i2b2demodata.relation set right_subject_id = min_pos_patient_num + right_subject_id where right_subject_id < 0;
			update i2b2demodata.qt_patient_set_collection set patient_num = min_pos_patient_num + patient_num where patient_num < 0;
			update deapp.de_subject_sample_mapping set patient_id = min_pos_patient_num + patient_id where patient_id < 0;
			update deapp.de_subject_microarray_data set patient_id = min_pos_patient_num + patient_id where patient_id < 0;

			RAISE NOTICE 'Negative patient identifiers were moved between 0 and min. patient id.';
		ELSE
			update i2b2demodata.patient_dimension set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update i2b2demodata.patient_mapping set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update i2b2demodata.patient_trial set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update i2b2demodata.visit_dimension set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update i2b2demodata.observation_fact set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update i2b2demodata.relation set left_subject_id = max_pos_patient_num + ABS(left_subject_id) where left_subject_id < 0;
			update i2b2demodata.relation set right_subject_id = max_pos_patient_num + ABS(right_subject_id) where right_subject_id < 0;
			update i2b2demodata.qt_patient_set_collection set patient_num = max_pos_patient_num + ABS(patient_num) where patient_num < 0;
			update deapp.de_subject_sample_mapping set patient_id = max_pos_patient_num + ABS(patient_id) where patient_id < 0;
			update deapp.de_subject_microarray_data set patient_id = max_pos_patient_num + ABS(patient_id) where patient_id < 0;
			perform setval('i2b2demodata.SEQ_PATIENT_NUM'::regclass, (max_pos_patient_num + ABS(min_neg_patient_num))::bigint);
			RAISE NOTICE 'Negative patient identifiers were moved after max. patient id.';
		END IF;
		refresh materialized view biomart_user.study_concept_bitset;
	END IF;
END$$;
COMMIT; -- OR ROLLBACK

