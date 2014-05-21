--
-- Name: lz_src_mrna_subj_samp_map; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lz_src_mrna_subj_samp_map (
    trial_name character varying(100),
    site_id character varying(100),
    subject_id character varying(100),
    sample_cd character varying(100),
    platform character varying(100),
    tissue_type character varying(100),
    attribute_1 character varying(256),
    attribute_2 character varying(200),
    category_cd character varying(200),
    source_cd character varying(200)
);

--
-- Name: lz_src_mrna_subj_samp_idx1; Type: INDEX; Schema: tm_lz; Owner: -
--
CREATE INDEX lz_src_mrna_subj_samp_idx1 ON lz_src_mrna_subj_samp_map USING btree (trial_name, source_cd);

