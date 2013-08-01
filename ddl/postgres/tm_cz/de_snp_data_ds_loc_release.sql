--
-- Name: de_snp_data_ds_loc_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE de_snp_data_ds_loc_release (
    snp_data_dataset_loc_id bigint,
    trial_name character varying(255),
    snp_dataset_id bigint,
    location bigint,
    release_study character varying(200)
);

