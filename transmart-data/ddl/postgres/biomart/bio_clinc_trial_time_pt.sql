--
-- Name: bio_clinc_trial_time_pt; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_clinc_trial_time_pt (
    bio_clinc_trial_tm_pt_id bigint NOT NULL,
    time_point character varying(200),
    time_point_code character varying(200),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    bio_experiment_id bigint NOT NULL
);

--
-- Name: bio_clinc_trial_time_pt bio_clinical_trial_time_point_; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_time_pt
    ADD CONSTRAINT bio_clinical_trial_time_point_ PRIMARY KEY (bio_clinc_trial_tm_pt_id);

--
-- Name: bio_clinc_trial_time_pt_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_clinc_trial_time_pt_pk ON bio_clinc_trial_time_pt USING btree (bio_clinc_trial_tm_pt_id);

--
-- Name: tf_trg_bio_cl_trl_time_pt_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_cl_trl_time_pt_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CLINC_TRIAL_TM_PT_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CLINC_TRIAL_TM_PT_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_clinc_trial_time_pt trg_bio_cl_trl_time_pt_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_cl_trl_time_pt_id BEFORE INSERT ON bio_clinc_trial_time_pt FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_cl_trl_time_pt_id();

--
-- Name: bio_clinc_trial_time_pt bio_cli_trial_time_trl_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinc_trial_time_pt
    ADD CONSTRAINT bio_cli_trial_time_trl_fk FOREIGN KEY (bio_experiment_id) REFERENCES bio_clinical_trial(bio_experiment_id);

