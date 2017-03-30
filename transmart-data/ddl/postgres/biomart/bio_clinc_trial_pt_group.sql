--
-- Name: bio_clinc_trial_pt_group; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_clinc_trial_pt_group (
    bio_experiment_id bigint NOT NULL,
    bio_clinical_trial_p_group_id bigint NOT NULL,
    name character varying(510),
    description character varying(1000),
    number_of_patients integer,
    patient_group_type_code character varying(200)
);

--
-- Name: bio_clinc_trial_pt_group bio_clinical_trial_pt_group; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_pt_group
    ADD CONSTRAINT bio_clinical_trial_pt_group PRIMARY KEY (bio_clinical_trial_p_group_id);

--
-- Name: bio_clinc_trial_pt_group_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_clinc_trial_pt_group_pk ON bio_clinc_trial_pt_group USING btree (bio_clinical_trial_p_group_id);

--
-- Name: tf_trg_bio_clin_trl_pt_grp_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_clin_trl_pt_grp_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CLINICAL_TRIAL_P_GROUP_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CLINICAL_TRIAL_P_GROUP_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_clinc_trial_pt_group trg_bio_clin_trl_pt_grp_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_clin_trl_pt_grp_id BEFORE INSERT ON bio_clinc_trial_pt_group FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_clin_trl_pt_grp_id();

--
-- Name: bio_clinc_trial_pt_group bio_clinc_trl_pt_grp_exp_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_pt_group
    ADD CONSTRAINT bio_clinc_trl_pt_grp_exp_fk FOREIGN KEY (bio_experiment_id) REFERENCES bio_clinical_trial(bio_experiment_id);

