--
-- Name: bio_clinical_trial_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE bio_clinical_trial_release (
    trial_number character varying(510),
    study_owner character varying(510),
    study_phase character varying(100),
    blinding_procedure character varying(1000),
    studytype character varying(510),
    duration_of_study_weeks integer,
    number_of_patients integer,
    number_of_sites integer,
    route_of_administration character varying(510),
    dosing_regimen character varying(3500),
    group_assignment character varying(510),
    type_of_control character varying(510),
    completion_date timestamp without time zone,
    primary_end_points character varying(2000),
    secondary_end_points character varying(3500),
    inclusion_criteria text,
    exclusion_criteria text,
    subjects character varying(2000),
    gender_restriction_mfb character varying(510),
    min_age integer,
    max_age integer,
    secondary_ids character varying(510),
    bio_experiment_id bigint,
    development_partner character varying(100),
    geo_platform character varying(30),
    main_findings character varying(2000),
    platform_name character varying(200),
    search_area character varying(100),
    release_study character varying(510)
);

