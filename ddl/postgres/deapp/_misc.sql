--
-- Name: TABLE de_variant_metadata; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON TABLE de_variant_metadata IS 'Contains meta information from the headers of a VCF file. Each header with meta information consists of a key and value.';

--
-- Name: seq_assay_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_assay_id
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: seq_mrna_partition_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_mrna_partition_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

