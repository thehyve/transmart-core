--
-- Name: ioe_temp_obj_reg2; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE ioe_temp_obj_reg2 (
    object_schema character varying(32),
    object_name character varying(32),
    object_type character varying(64),
    kde_username character varying(64),
    kde_session character varying(255),
    object_created_timestamp timestamp(0) without time zone
);

