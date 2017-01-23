--
-- Name: sq_up_patdim_patientnum; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE sq_up_patdim_patientnum
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: set_type; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE set_type (
    id integer DEFAULT nextval('sq_up_patdim_patientnum'::regclass) NOT NULL,
    name character varying(500),
    create_date timestamp without time zone
);

--
-- Name: pk_st_id; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY set_type
    ADD CONSTRAINT pk_st_id PRIMARY KEY (id);

