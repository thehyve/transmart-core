--
-- Name: concept_counts; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE concept_counts (
    concept_path character varying(500),
    parent_concept_path character varying(500),
    patient_count numeric(18,0)
);

