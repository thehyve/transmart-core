--
-- Name: de_variant_subject_idx_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_subject_idx_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_subject_idx; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_subject_idx (
    dataset_id character varying(50),
    subject_id character varying(50),
    "position" bigint,
    variant_subject_idx_id bigint DEFAULT nextval('de_variant_subject_idx_seq'::regclass) NOT NULL
);

--
-- Name: variant_subject_idx_uk; Type: INDEX; Schema: deapp; Owner: -
--
CREATE UNIQUE INDEX variant_subject_idx_uk ON de_variant_subject_idx USING btree (dataset_id, subject_id, "position");

--
-- Name: de_variant_subject_idx variant_subject_idx_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_subject_idx
    ADD CONSTRAINT variant_subject_idx_fk FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

