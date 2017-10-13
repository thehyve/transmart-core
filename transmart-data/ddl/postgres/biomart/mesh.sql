--
-- Name: mesh; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mesh (
    ui character varying(20) NOT NULL,
    mh character varying(200),
    mn character varying(200),
    ui_path character varying(500)
);

--
-- Name: mesh mesh_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mesh
    ADD CONSTRAINT mesh_pkey PRIMARY KEY (ui);

--
-- Name: mesh_idx_mn; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX mesh_idx_mn ON mesh USING btree (mn);

--
-- Name: mesh_idx_ui; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX mesh_idx_ui ON mesh USING btree (ui);

