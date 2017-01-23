--
-- Name: lz_src_study_metadata_ad_hoc; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lz_src_study_metadata_ad_hoc (
    study_id character varying(100),
    ad_hoc_property_key character varying(500),
    ad_hoc_property_value character varying(4000),
    ad_hoc_property_link character varying(500),
    upload_date timestamp without time zone
);

