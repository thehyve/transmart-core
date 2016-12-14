--
-- Name: storage_system; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE storage_system (
    id integer NOT NULL,
    name character varying(50),
    systemtype character varying(50),
    url character varying(900),
    system_version character varying(50),
    single_file_collections boolean
);

--
-- Name: storage_system_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY storage_system
    ADD CONSTRAINT storage_system_pkey PRIMARY KEY (id);

