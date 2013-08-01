--
-- Name: rwg_analysis_data_ext; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE rwg_analysis_data_ext (
    analysis_id character varying(100),
    probeset character varying(100),
    preferred_pvalue character varying(100),
    raw_pvalue character varying(100),
    adjusted_pvalue character varying(100),
    fold_change character varying(100),
    lsmean_1 character varying(100),
    lsmean_2 character varying(100)
);

