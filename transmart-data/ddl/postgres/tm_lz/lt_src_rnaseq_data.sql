--
-- Name: lt_src_rnaseq_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE tm_lz.lt_src_rnaseq_data
(
  trial_name character varying(25),
  region_name character varying(100),
  expr_id character varying(100),
  readcount character varying(50),
  normalized_readcount character varying(50)
);

