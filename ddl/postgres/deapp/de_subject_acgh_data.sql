--
-- Name: de_subject_acgh_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_acgh_data (
    trial_name character varying(50),
    region_id bigint NOT NULL,
    assay_id bigint NOT NULL,
    patient_id bigint,
    chip double precision,
    segmented double precision,
    flag smallint,
    probloss double precision,
    probnorm double precision,
    probgain double precision,
    probamp double precision,
    partition_id numeric
);

--
-- Name: COLUMN de_subject_acgh_data.chip; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_acgh_data.chip IS 'log2ratio';

--
-- Name: COLUMN de_subject_acgh_data.segmented; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_acgh_data.segmented IS 'segmented log2ratio';

--
-- Name: COLUMN de_subject_acgh_data.flag; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_acgh_data.flag IS 'call  -1:loss, 0:normal, 1:gain, 2:amplification';

--
-- Name: de_subject_acgh_data_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_acgh_data
    ADD CONSTRAINT de_subject_acgh_data_pkey PRIMARY KEY (assay_id, region_id);

--
-- Name: de_subject_acgh_data_patient; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_acgh_data_patient ON de_subject_acgh_data USING btree (patient_id);

--
-- Name: de_subject_acgh_data_region; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_acgh_data_region ON de_subject_acgh_data USING btree (region_id);

--
-- Name: de_subj_acgh_region_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_acgh_data
    ADD CONSTRAINT de_subj_acgh_region_id_fkey FOREIGN KEY (region_id) REFERENCES de_chromosomal_region(region_id);

--
-- Name: de_subject_acgh_data_assay_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_acgh_data
    ADD CONSTRAINT de_subject_acgh_data_assay_id_fk FOREIGN KEY (assay_id) REFERENCES de_subject_sample_mapping(assay_id) ON DELETE CASCADE;

