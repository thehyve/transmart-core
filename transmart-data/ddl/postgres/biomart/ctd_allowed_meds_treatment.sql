--
-- Name: ctd_allowed_meds_treatment; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd_allowed_meds_treatment (
    ctd_study_id bigint,
    trtmt_ocs character varying(4000),
    trtmt_ics character varying(4000),
    trtmt_laba character varying(4000),
    trtmt_ltra character varying(4000),
    trtmt_corticosteroids character varying(4000),
    trtmt_anti_fibrotics character varying(4000),
    trtmt_immunosuppressive character varying(4000),
    trtmt_cytotoxic character varying(4000)
);

