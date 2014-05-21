--
-- Name: mesh; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mesh (
    ui character varying(20) NOT NULL,
    mh character varying(200),
    mn character varying(200)
);

--
-- Name: mesh_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mesh
    ADD CONSTRAINT mesh_pkey PRIMARY KEY (ui);

