--
-- Name: qt_sq_qper_pecid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qper_pecid
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_patient_enc_collection; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_patient_enc_collection (
    patient_enc_coll_id numeric(10,0) DEFAULT nextval('qt_sq_qper_pecid'::regclass) NOT NULL,
    result_instance_id numeric(5,0),
    set_index numeric(10,0),
    patient_num numeric(10,0),
    encounter_num numeric(10,0)
);

--
-- Name: qt_patient_enc_coll_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_patient_enc_collection
    ADD CONSTRAINT qt_patient_enc_coll_pk PRIMARY KEY (patient_enc_coll_id);

--
-- Name: qt_fk_pec_ri; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_patient_enc_collection
    ADD CONSTRAINT qt_fk_pec_ri FOREIGN KEY (result_instance_id) REFERENCES qt_query_result_instance(result_instance_id);

