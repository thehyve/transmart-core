--
-- Name: wt_subject_metabolomics_calcs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_metabolomics_calcs (
    trial_name character varying(50),
    probeset character varying(500),
    mean_intensity bigint,
    median_intensity bigint,
    stddev_intensity bigint
);

