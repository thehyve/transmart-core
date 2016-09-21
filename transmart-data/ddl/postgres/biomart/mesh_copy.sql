--
-- Name: mesh_copy; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mesh_copy (
    ui character varying(20) NOT NULL,
    mh character varying(200),
    mn character varying(200)
);

--
-- Name: mesh_copy_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mesh_copy
    ADD CONSTRAINT mesh_copy_pkey PRIMARY KEY (ui);

