--
-- Name: tests; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE tests (
    name character varying(100),
    platform character varying(100),
    id bigint,
    test character varying(1000),
    probeset character varying(100),
    raw_pvalue double precision,
    adjusted_pvalue double precision,
    estimate double precision,
    fold_change double precision,
    max_ls_mean double precision
);

