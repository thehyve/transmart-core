--
-- Name: qt_sq_qpr_pcid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qpr_pcid
    START WITH 4430157
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_patient_set_collection; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_patient_set_collection (
    patient_set_coll_id numeric(10,0) DEFAULT nextval('qt_sq_qpr_pcid'::regclass) NOT NULL,
    result_instance_id numeric(5,0),
    set_index numeric(10,0),
    patient_num numeric(10,0)
);

--
-- Name: qt_patient_set_collection_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_patient_set_collection
    ADD CONSTRAINT qt_patient_set_collection_pkey PRIMARY KEY (patient_set_coll_id);

--
-- Name: qt_idx_qpsc_riid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_qpsc_riid ON qt_patient_set_collection USING btree (result_instance_id);

--
-- Name: qt_idx_qpsc_riid_pn; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_qpsc_riid_pn ON qt_patient_set_collection USING btree (result_instance_id, patient_num);

--
-- Name: qt_fk_psc_ri; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_patient_set_collection
    ADD CONSTRAINT qt_fk_psc_ri FOREIGN KEY (result_instance_id) REFERENCES qt_query_result_instance(result_instance_id);

