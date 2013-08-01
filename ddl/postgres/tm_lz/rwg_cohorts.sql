--
-- Name: rwg_cohorts; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE rwg_cohorts (
    study_id character varying(500),
    cohort_id character varying(500),
    disease character varying(500),
    sample_type character varying(500),
    treatment character varying(500),
    organism character varying(500),
    pathology character varying(500),
    cohort_title character varying(500),
    short_desc character varying(500),
    long_desc character varying(500),
    import_date timestamp(6) without time zone DEFAULT now() NOT NULL
);

