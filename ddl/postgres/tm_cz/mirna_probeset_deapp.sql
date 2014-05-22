--
-- Name: seq_probeset_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_probeset_id
    START WITH 5461
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 99999999
    CACHE 20;

--
-- Name: mirna_probeset_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE mirna_probeset_deapp (
    probeset_id numeric(38,0) DEFAULT nextval('seq_probeset_id'::regclass) NOT NULL,
    probeset character varying(100),
    platform character varying(100),
    organism character varying(200)
);

