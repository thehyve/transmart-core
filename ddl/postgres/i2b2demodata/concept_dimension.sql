--
-- Name: concept_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE concept_dimension (
    concept_cd character varying(50) NOT NULL,
    concept_path character varying(700) NOT NULL,
    name_char character varying(2000),
    concept_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    table_name character varying(255)
);

--
-- Name: concept_dimension_concept_cd_uq; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY concept_dimension
    ADD CONSTRAINT concept_dimension_concept_cd_uq UNIQUE (concept_cd);

--
-- Name: concept_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY concept_dimension
    ADD CONSTRAINT concept_dimension_pk PRIMARY KEY (concept_path);

--
-- Name: cd_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX cd_uploadid_idx ON concept_dimension USING btree (upload_id);

--
-- Name: tf_trg_concept_dimension_cd(); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION tf_trg_concept_dimension_cd() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	 if NEW.CONCEPT_CD is null then
	 select nextval('CONCEPT_ID') into NEW.CONCEPT_CD;
	 end if;
	 RETURN NEW;
	 end;

$$;

--
-- Name: trg_concept_dimension_cd; Type: TRIGGER; Schema: i2b2demodata; Owner: -
--
CREATE TRIGGER trg_concept_dimension_cd BEFORE INSERT ON concept_dimension FOR EACH ROW EXECUTE PROCEDURE tf_trg_concept_dimension_cd();

--
-- Name: concept_id; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE concept_id
    START WITH 1340964
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

