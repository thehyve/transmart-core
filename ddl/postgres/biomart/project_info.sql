--
-- Name: project_info; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE project_info (
    file_id integer NOT NULL,
    id character varying(100),
    name character varying(100),
    file_name character varying(100),
    activedataname character varying(400),
    project_accession character varying(100),
    project_category text,
    project_contactaddress character varying(4000),
    project_contactcompany character varying(400),
    project_contactdepartment character varying(400),
    project_contactemail character varying(400),
    project_contactlaboratory character varying(400),
    project_contactname character varying(400),
    project_contactphone character varying(100),
    project_contactweblink character varying(1000),
    project_contributors character varying(4000),
    project_description text,
    project_design text,
    project_id character varying(100),
    project_keywords character varying(1000),
    project_organism character varying(400),
    project_tissue character varying(400),
    project_compound character varying(400),
    project_platform character varying(400),
    project_platformdescription character varying(4000),
    project_platformorganism character varying(400),
    project_platformprovider character varying(400),
    project_platformtechnology character varying(400),
    project_platformtype character varying(400),
    project_pubmed character varying(400),
    project_studytype character varying(1000),
    project_supplementaryfile character varying(400),
    project_title character varying(500),
    project_weblink character varying(1000),
    project_extractedfromcelfiles character varying(1000),
    project_contactorganization character varying(1000),
    project_contactfax character varying(100),
    project_outputfile character varying(500),
    project_datasource character varying(100),
    project_editors character varying(100),
    project_isprivate character varying(100),
    project_publishdate character varying(100),
    entrydt timestamp without time zone
);

--
-- Name: project_info_file_name_key; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY project_info
    ADD CONSTRAINT project_info_file_name_key UNIQUE (file_name);

--
-- Name: project_info_id_key; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY project_info
    ADD CONSTRAINT project_info_id_key UNIQUE (id);

--
-- Name: project_info_name_key; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY project_info
    ADD CONSTRAINT project_info_name_key UNIQUE (name);

--
-- Name: project_info_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY project_info
    ADD CONSTRAINT project_info_pkey PRIMARY KEY (file_id);

