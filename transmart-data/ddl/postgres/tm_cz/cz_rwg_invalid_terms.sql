--
-- Name: cz_rwg_invalid_terms; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_rwg_invalid_terms (
    study_id character varying(500),
    category_name character varying(500),
    term_name character varying(500),
    import_date timestamp without time zone DEFAULT now()
);

