--
-- Name: bio_regulome_score; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_regulome_score (
    chromosome character varying(20),
    "position" numeric(18,0),
    rs_id character varying(100),
    score character varying(10)
);

