--
-- Name: haploview_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE haploview_data (
    i2b2_id bigint,
    jnj_id character varying(30),
    father_id integer,
    mother_id integer,
    sex smallint,
    affection_status smallint,
    chromosome character varying(10),
    gene character varying(50),
    release smallint,
    release_date timestamp without time zone,
    trial_name character varying(50),
    snp_data text
);

