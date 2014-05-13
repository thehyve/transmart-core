-- Type: SEQUENCE; Owner: DEAPP; Name: SEQ_DE_MRNA_ANNOTATION_ID
--
CREATE SEQUENCE seq_de_mrna_annotation_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 111121
    CACHE 1
;

--
-- Name: de_mrna_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_mrna_annotation (
    gpl_id character varying(100),
    probe_id character varying(100),
    gene_symbol character varying(100),
    probeset_id bigint,
    gene_id bigint,
    organism character varying(200),
    de_mrna_annotation_id bigint NOT NULL,
    PRIMARY KEY (de_mrna_annotation_id)
);

--
-- Name: de_mrna_annotation_index1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_mrna_annotation_index1 ON de_mrna_annotation USING btree (probeset_id);

