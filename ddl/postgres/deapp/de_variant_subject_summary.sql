--
-- Name: de_variant_subject_summary_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_subject_summary_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_subject_summary; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_subject_summary (
    variant_subject_summary_id bigint DEFAULT nextval('de_variant_subject_summary_seq'::regclass) NOT NULL,
    chr character varying(50),
    pos bigint,
    dataset_id character varying(50) NOT NULL,
    subject_id character varying(50) NOT NULL,
    rs_id character varying(500),
    variant character varying(1000),
    variant_format character varying(100),
    variant_type character varying(100),
    reference boolean,
    allele1 integer,
    allele2 integer,
    assay_id bigint
);

--
-- Name: COLUMN de_variant_subject_summary.reference; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_variant_subject_summary.reference IS 'This column contains a flag whether this subject has a reference value on this variant, or not.';

--
-- Name: COLUMN de_variant_subject_summary.assay_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_variant_subject_summary.assay_id IS 'Reference to de_subject_sample_mapping';

--
-- Name: variant_subject_summary_id; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_subject_summary
    ADD CONSTRAINT variant_subject_summary_id PRIMARY KEY (variant_subject_summary_id);

--
-- Name: gen_variant_subj_summ_chr_pos; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX gen_variant_subj_summ_chr_pos ON de_variant_subject_summary USING btree (chr, pos);

--
-- Name: variant_subject_summary_uk; Type: INDEX; Schema: deapp; Owner: -
--
CREATE UNIQUE INDEX variant_subject_summary_uk ON de_variant_subject_summary USING btree (dataset_id, chr, pos, rs_id, subject_id);

--
-- Name: variant_subject_summary_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_subject_summary
    ADD CONSTRAINT variant_subject_summary_fk FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

