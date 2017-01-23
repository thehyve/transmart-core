--
-- Name: ctd_disease; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd_disease (
    ctd_study_id bigint,
    common_name character varying(4000),
    icd10 character varying(4000),
    mesh character varying(4000),
    disease_severity character varying(4000)
);

