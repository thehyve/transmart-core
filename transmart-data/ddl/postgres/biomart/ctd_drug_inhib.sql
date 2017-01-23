--
-- Name: ctd_drug_inhib; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd_drug_inhib (
    ctd_study_id bigint,
    drug_inhibitor_common_name character varying(4000),
    drug_inhibitor_standard_name character varying(4000),
    drug_inhibitor_cas_id character varying(4000)
);

