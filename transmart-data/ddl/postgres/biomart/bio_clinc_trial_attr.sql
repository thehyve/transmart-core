--
-- Name: bio_clinc_trial_attr; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_clinc_trial_attr (
    bio_clinc_trial_attr_id bigint NOT NULL,
    property_code character varying(200) NOT NULL,
    property_value character varying(200),
    bio_experiment_id bigint NOT NULL
);

--
-- Name: bio_clinc_trial_attr bio_clinical_trial_patient_grp; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_attr
    ADD CONSTRAINT bio_clinical_trial_patient_grp PRIMARY KEY (bio_clinc_trial_attr_id);

--
-- Name: bio_clinc_trial_attr_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_clinc_trial_attr_pk ON bio_clinc_trial_attr USING btree (bio_clinc_trial_attr_id);

--
-- Name: tf_trg_bio_cln_trl_attr_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_cln_trl_attr_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CLINC_TRIAL_ATTR_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CLINC_TRIAL_ATTR_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_clinc_trial_attr trg_bio_cln_trl_attr_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_cln_trl_attr_id BEFORE INSERT ON bio_clinc_trial_attr FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_cln_trl_attr_id();

--
-- Name: bio_clinc_trial_attr bio_clinical_trial_property_bi; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_attr
    ADD CONSTRAINT bio_clinical_trial_property_bi FOREIGN KEY (bio_experiment_id) REFERENCES bio_clinical_trial(bio_experiment_id);

