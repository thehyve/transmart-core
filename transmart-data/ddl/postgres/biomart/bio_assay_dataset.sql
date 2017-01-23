--
-- Name: bio_assay_dataset; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_dataset (
    bio_assay_dataset_id bigint NOT NULL,
    dataset_name character varying(400),
    dataset_description character varying(1000),
    dataset_criteria character varying(1000),
    create_date timestamp without time zone,
    bio_experiment_id bigint NOT NULL,
    bio_assay_id bigint,
    etl_id character varying(100),
    accession character varying(50)
);

--
-- Name: bio_dataset_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_dataset
    ADD CONSTRAINT bio_dataset_pk PRIMARY KEY (bio_assay_dataset_id);

--
-- Name: bio_assay_dataset_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_assay_dataset_pk ON bio_assay_dataset USING btree (bio_assay_dataset_id);

--
-- Name: tf_trg_bio_assay_dataset_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_assay_dataset_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_ASSAY_DATASET_ID is null then
         select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASSAY_DATASET_ID ;
    end if;
RETURN NEW;
end;

$$;

--
-- Name: trg_bio_assay_dataset_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_assay_dataset_id BEFORE INSERT ON bio_assay_dataset FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_assay_dataset_id();

--
-- Name: bio_dataset_experiment_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_dataset
    ADD CONSTRAINT bio_dataset_experiment_fk FOREIGN KEY (bio_experiment_id) REFERENCES bio_experiment(bio_experiment_id);

