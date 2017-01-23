--
-- Name: lz_src_study_metadata; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lz_src_study_metadata (
    study_title character varying(500),
    study_date character varying(50),
    study_owner character varying(500),
    study_institution character varying(500),
    study_country character varying(500),
    study_related_publication character varying(500),
    study_description character varying(2000),
    study_access_type character varying(500),
    study_phase character varying(500),
    study_objective character varying(2000),
    study_biomarker_type character varying(500),
    study_compound character varying(500),
    study_design_factors character varying(2000),
    study_nbr_subjects character varying(20),
    study_organism character varying(500),
    study_id character varying(50)
);
