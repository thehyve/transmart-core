--
-- Name: supported_workflow; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE supported_workflow
  (
  id INTEGER NOT NULL,
  name character varying(500),
  description character varying(900),
  uuid character varying(50) NOT NULL,
  arvados_instance_url character varying(900),
  arvados_version character varying(50),
  default_params TEXT
  );

  --
-- Name: supported_workflow_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY supported_workflow
    ADD CONSTRAINT supported_workflow_pkey PRIMARY KEY (id);
