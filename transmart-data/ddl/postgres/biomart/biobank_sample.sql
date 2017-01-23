--
-- Name: biobank_sample; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE biobank_sample (
    sample_tube_id character varying(255) NOT NULL,
    accession_number character varying(255) NOT NULL,
    client_sample_tube_id character varying(255) NOT NULL,
    container_id character varying(255) NOT NULL,
    import_date timestamp without time zone,
    source_type character varying(255) NOT NULL
);

--
-- Name: biobank_sample_pkey; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY biobank_sample
    ADD CONSTRAINT biobank_sample_pkey PRIMARY KEY (sample_tube_id);

