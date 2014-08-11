--
-- Name: bio_experiment; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_experiment (
    bio_experiment_id bigint NOT NULL,
    bio_experiment_type character varying(200),
    title character varying(1000),
    description character varying(4000),
    design character varying(2000),
    start_date timestamp without time zone,
    completion_date timestamp without time zone,
    primary_investigator character varying(400),
    contact_field character varying(400),
    etl_id character varying(100),
    status character varying(100),
    overall_design character varying(2000),
    accession character varying(100),
    entrydt timestamp without time zone,
    updated timestamp without time zone,
    institution character varying(400),
    country character varying(1000),
    biomarker_type character varying(255),
    target character varying(255),
    access_type character varying(100)
);

--
-- Name: experimentdim_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_experiment
    ADD CONSTRAINT experimentdim_pk PRIMARY KEY (bio_experiment_id);

--
-- Name: bio_exp_acen_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_exp_acen_idx ON bio_experiment USING btree (accession);

--
-- Name: bio_exp_type_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_exp_type_idx ON bio_experiment USING btree (bio_experiment_type);

--
-- Name: bio_experiment_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_experiment_pk ON bio_experiment USING btree (bio_experiment_id);

--
-- Name: tf_trg_bio_experiment_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_experiment_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.BIO_EXPERIMENT_ID is null then
         select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_EXPERIMENT_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_experiment_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_experiment_id BEFORE INSERT ON bio_experiment FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_experiment_id();

--
-- Name: tf_trg_bio_experiment_uid(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_experiment_uid() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM biomart.bio_data_uid 
  WHERE bio_data_id = NEW.bio_experiment_id;
  
  if rec_count = 0 then
    insert into biomart.bio_data_uid (bio_data_id, unique_id, bio_data_type)
    values (NEW.bio_experiment_id, biomart.bio_experiment_uid(NEW.accession), 'BIO_EXPERIMENT');
  end if;
RETURN NEW;
END;
$$;

--
-- Name: trg_bio_experiment_uid; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_experiment_uid BEFORE INSERT ON bio_experiment FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_experiment_uid();

