--
-- Name: relation; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE i2b2demodata.relation (
    left_subject_id NUMERIC(38,0) NOT NULL,
    relation_type_id INTEGER NOT NULL,
    right_subject_id NUMERIC(38,0) NOT NULL,
    biological BOOLEAN,
    share_household BOOLEAN
);

--
-- Name: relation_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY i2b2demodata.relation
    ADD CONSTRAINT relation_pk PRIMARY KEY (left_subject_id, relation_type_id, right_subject_id);

--
-- Name: left_subject_id_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY i2b2demodata.relation
    ADD CONSTRAINT left_subject_id_fk FOREIGN KEY (left_subject_id) REFERENCES i2b2demodata.patient_dimension(patient_num);

--
-- Name: relation_type_id_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY i2b2demodata.relation
    ADD CONSTRAINT relation_type_id_fk FOREIGN KEY (relation_type_id) REFERENCES i2b2demodata.relation_type(id);


--
-- Name: right_subject_id_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY i2b2demodata.relation
    ADD CONSTRAINT right_subject_id_fk FOREIGN KEY (right_subject_id) REFERENCES i2b2demodata.patient_dimension(patient_num);

COMMENT ON TABLE i2b2demodata.relation IS 'Represents relationships between subjects. e.g. pedigree.';

COMMENT ON COLUMN i2b2demodata.relation.left_subject_id IS 'Id of the left subject in the relation.';
COMMENT ON COLUMN i2b2demodata.relation.relation_type_id IS 'Relation type id.';
COMMENT ON COLUMN i2b2demodata.relation.right_subject_id IS 'Id of the right subject in the relation.';
COMMENT ON COLUMN i2b2demodata.relation.biological IS 'Specifies whether relation biological.';
COMMENT ON COLUMN i2b2demodata.relation.share_household IS 'Specifies whether subjects share household.';