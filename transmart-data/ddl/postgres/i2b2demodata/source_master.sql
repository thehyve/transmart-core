--
-- Name: source_master; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE source_master (
    source_cd character varying(50) NOT NULL,
    description character varying(300),
    create_date timestamp without time zone
);

--
-- Name: pk_sourcemaster_sourcecd; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY source_master
    ADD CONSTRAINT pk_sourcemaster_sourcecd PRIMARY KEY (source_cd);

