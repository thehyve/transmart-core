--
-- Name: mirna_bio_marker; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mirna_bio_marker (
    bio_marker_id bigint NOT NULL,
    bio_marker_name character varying(200),
    bio_marker_description character varying(1000),
    organism character varying(200),
    primary_source_code character varying(200),
    primary_external_id character varying(200),
    bio_marker_type character varying(200) NOT NULL
);

--
-- Name: mirna_bio_marker_organism_primary_external_id_key; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mirna_bio_marker
    ADD CONSTRAINT mirna_bio_marker_organism_primary_external_id_key UNIQUE (organism, primary_external_id);

