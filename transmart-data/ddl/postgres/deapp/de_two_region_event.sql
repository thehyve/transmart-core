--
-- Name: de_two_region_event_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_two_region_event_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_two_region_event; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_two_region_event (
    two_region_event_id bigint DEFAULT nextval('de_two_region_event_seq'::regclass) NOT NULL,
    cga_type character varying(500),
    soap_class character varying(500)
);

--
-- Name: COLUMN de_two_region_event.cga_type; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_event.cga_type IS 'deletion, inversion, duplication,... Type from http://cgatools.sourceforge.net/docs/1.8.0/cgatools-command-line-reference.html#junctions2events';

--
-- Name: COLUMN de_two_region_event.soap_class; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_event.soap_class IS 'inter/intra chromosomal inversion/translocation: http://sourceforge.net/p/soapfuse/wiki/classification-of-fusions.for.SOAPfuse/';

--
-- Name: two_region_event_id_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_event
    ADD CONSTRAINT two_region_event_id_pk PRIMARY KEY (two_region_event_id);

