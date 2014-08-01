--
-- Name: mesh_path; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mesh_path (
    unique_id character varying(20) NOT NULL,
    mesh_name character varying(200),
    child_number character varying(200),
    parent_number character varying(200),
    id_path character varying(500)
);

--
-- Name: mesh_path_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mesh_path
    ADD CONSTRAINT mesh_path_pkey PRIMARY KEY (unique_id);

