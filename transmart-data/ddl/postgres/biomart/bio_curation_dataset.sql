--
-- Name: bio_curation_dataset; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_curation_dataset (
    bio_curation_dataset_id bigint NOT NULL,
    bio_asy_analysis_pltfm_id bigint,
    bio_source_import_id bigint,
    bio_curation_type character varying(200) NOT NULL,
    create_date timestamp without time zone,
    creator bigint,
    bio_curation_name character varying(500),
    data_type character varying(100)
);

--
-- Name: bio_external_analysis_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_curation_dataset
    ADD CONSTRAINT bio_external_analysis_pk PRIMARY KEY (bio_curation_dataset_id);

--
-- Name: bio_curation_dataset_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_curation_dataset_pk ON bio_curation_dataset USING btree (bio_curation_dataset_id);

--
-- Name: tf_trg_bio_curation_dataset_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_curation_dataset_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.BIO_CURATION_DATASET_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CURATION_DATASET_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_curation_dataset_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_curation_dataset_id BEFORE INSERT ON bio_curation_dataset FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_curation_dataset_id();

--
-- Name: bio_ext_anl_pltfm_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_curation_dataset
    ADD CONSTRAINT bio_ext_anl_pltfm_fk FOREIGN KEY (bio_asy_analysis_pltfm_id) REFERENCES bio_asy_analysis_pltfm(bio_asy_analysis_pltfm_id);

