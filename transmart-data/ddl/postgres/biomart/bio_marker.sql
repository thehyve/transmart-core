--
-- Name: bio_marker; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_marker (
    bio_marker_id bigint NOT NULL,
    bio_marker_name character varying(200),
    bio_marker_description character varying(1000),
    organism character varying(200),
    primary_source_code character varying(200),
    primary_external_id character varying(200),
    bio_marker_type character varying(200) NOT NULL
);

--
-- Name: bio_marker biomarker_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_marker
    ADD CONSTRAINT biomarker_pk PRIMARY KEY (bio_marker_id);

--
-- Name: bio_marker biomarker_uk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_marker
    ADD CONSTRAINT biomarker_uk UNIQUE (organism, primary_external_id);

--
-- Name: bio_marker_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_marker_pk ON bio_marker USING btree (bio_marker_id);

--
-- Name: bio_mkr_ext_id; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_mkr_ext_id ON bio_marker USING btree (primary_external_id);

--
-- Name: bio_mkr_src_ext_id_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_mkr_src_ext_id_idx ON bio_marker USING btree (primary_source_code, primary_external_id);

--
-- Name: bio_mkr_type_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_mkr_type_idx ON bio_marker USING btree (bio_marker_type);

--
-- Name: tf_trg_bio_marker_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_marker_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin  if NEW.BIO_MARKER_ID is null then          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_MARKER_ID ;       end if;  RETURN NEW;  end;
$$;

--
-- Name: bio_marker trg_bio_marker_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_marker_id BEFORE INSERT ON bio_marker FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_marker_id();

