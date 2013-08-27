--
-- Name: de_chromosomal_region_region_id_seq; Type: SEQUENCE OWNED BY; Schema: deapp; Owner: -
--
ALTER SEQUENCE de_chromosomal_region_region_id_seq OWNED BY de_chromosomal_region.region_id;

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

