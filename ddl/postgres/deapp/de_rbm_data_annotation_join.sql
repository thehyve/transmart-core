--
-- Name: de_rbm_data_annotation_join; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rbm_data_annotation_join (
    data_id bigint,
    annotation_id bigint
);
--
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: SYS_C0010756
--
ALTER TABLE de_rbm_data_annotation_join ADD FOREIGN KEY (data_id)
 REFERENCES de_subject_rbm_data(id) ON DELETE CASCADE;
--
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: SYS_C0010748
--
ALTER TABLE de_rbm_data_annotation_join ADD FOREIGN KEY (annotation_id)
 REFERENCES de_rbm_annotation(id) ON DELETE CASCADE;

--
-- Name: pk_de_rbm_data_annotation_join; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT pk_de_rbm_data_annotation_join PRIMARY KEY (data_id, annotation_id);
