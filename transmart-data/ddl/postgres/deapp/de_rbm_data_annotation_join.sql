--
-- Name: de_rbm_data_annotation_join; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rbm_data_annotation_join (
    data_id bigint NOT NULL,
    annotation_id bigint NOT NULL
);

--
-- Name: de_rbm_data_annotation_join pk_de_rbm_data_annotation_join; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT pk_de_rbm_data_annotation_join PRIMARY KEY (data_id, annotation_id);

--
-- Name: de_rbm_data_annotation_join de_rbm_data_ann_jn_ann_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_data_annotation_join
    ADD CONSTRAINT de_rbm_data_ann_jn_ann_id_fk FOREIGN KEY (annotation_id) REFERENCES de_rbm_annotation(id) ON DELETE CASCADE;

