--
-- Name: wt_subject_proteomics_calcs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_proteomics_calcs (
    trial_name character varying(50),
    probeset_id character varying(500),
    mean_intensity numeric,
    median_intensity numeric,
    stddev_intensity numeric
);

