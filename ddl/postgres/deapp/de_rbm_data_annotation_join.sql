--
-- Name: de_rbm_data_annotation_join; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rbm_data_annotation_join (
    data_id bigint NOT NULL,
    annotation_id bigint NOT NULL
);

--
-- Name: pk_de_rbm_data_annotation_join; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT pk_de_rbm_data_annotation_join PRIMARY KEY (data_id, annotation_id);

--
-- Name: de_rbm_data_annotation_join_annotation_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT de_rbm_data_annotation_join_annotation_id_fkey FOREIGN KEY (annotation_id) REFERENCES de_rbm_annotation(id) ON DELETE CASCADE;

--
-- Name: de_rbm_data_annotation_join_data_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT de_rbm_data_annotation_join_data_id_fkey FOREIGN KEY (data_id) REFERENCES de_subject_rbm_data(id) ON DELETE CASCADE;

