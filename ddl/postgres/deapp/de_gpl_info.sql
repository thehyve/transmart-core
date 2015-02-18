--
-- Name: de_gpl_info; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_gpl_info (
    platform character varying(50) NOT NULL,
    title character varying(500),
    organism character varying(100),
    annotation_date timestamp without time zone,
    marker_type character varying(100),
    release_nbr character varying(50),
    genome_build character varying(20),
    gene_annotation_id character varying(50)
);

--
-- Name: de_gpl_info_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_gpl_info
    ADD CONSTRAINT de_gpl_info_pkey PRIMARY KEY (platform);

