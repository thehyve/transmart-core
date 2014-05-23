--
-- Name: wt_subject_mirna_calcs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_mirna_calcs (
    trial_name character varying(50),
    probeset_id numeric(38,0),
    mean_intensity numeric,
    median_intensity numeric,
    stddev_intensity numeric
);

