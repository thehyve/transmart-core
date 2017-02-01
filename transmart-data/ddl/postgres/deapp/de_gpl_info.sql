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

--
-- add documentation
--
COMMENT ON TABLE deapp.de_gpl_info IS 'Definition of GPL platforms';

COMMENT ON COLUMN de_gpl_info.platform IS 'Primary key. Platform id. E.g., GPL1000, GPL96, RNASEQ_TRANSCRIPT_PLATFORM.';
COMMENT ON COLUMN de_gpl_info.title IS 'Title of the platform. E.g., microarray test data, rnaseq transcript level test data.';
COMMENT ON COLUMN de_gpl_info.organism IS 'Organism the platform applies to. E.g., Human.';
COMMENT ON COLUMN de_gpl_info.marker_type IS 'E.g., Gene Expression, RNASEQ_TRANSCRIPT.';
COMMENT ON COLUMN de_gpl_info.genome_build IS 'E.g., hg19.';
