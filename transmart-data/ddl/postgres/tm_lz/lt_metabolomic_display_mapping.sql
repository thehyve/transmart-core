--
-- Name: lt_metabolomic_display_mapping; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_metabolomic_display_mapping (
    category_cd character varying(200) NOT NULL,
    display_value character varying(100),
    display_label character varying(200),
    display_unit character varying(20)
);
