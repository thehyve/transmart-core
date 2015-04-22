--
-- Name: de_variant_subject_detail_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_subject_detail_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_subject_detail; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_subject_detail (
    variant_subject_detail_id bigint DEFAULT nextval('de_variant_subject_detail_seq'::regclass) NOT NULL,
    dataset_id character varying(50),
    chr character varying(50),
    pos bigint,
    rs_id character varying(50),
    ref character varying(500),
    alt character varying(500),
    qual character varying(100),
    filter character varying(50),
    info character varying(10000),
    format character varying(500),
    variant_value text
);

--
-- Name: variant_subject_detail_id; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_subject_detail
    ADD CONSTRAINT variant_subject_detail_id PRIMARY KEY (variant_subject_detail_id);

--
-- Name: de_variant_sub_detail_idx2; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_variant_sub_detail_idx2 ON de_variant_subject_detail USING btree (dataset_id, chr);

--
-- Name: de_variant_sub_dt_idx1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_variant_sub_dt_idx1 ON de_variant_subject_detail USING btree (dataset_id, rs_id);

--
-- Name: gen_variant_subj_det_chr_pos; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX gen_variant_subj_det_chr_pos ON de_variant_subject_detail USING btree (chr, pos);

--
-- Name: variant_subject_detail_uk; Type: INDEX; Schema: deapp; Owner: -
--
CREATE UNIQUE INDEX variant_subject_detail_uk ON de_variant_subject_detail USING btree (dataset_id, chr, pos, rs_id);

--
-- Name: variant_subject_detail_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_subject_detail
    ADD CONSTRAINT variant_subject_detail_fk FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

