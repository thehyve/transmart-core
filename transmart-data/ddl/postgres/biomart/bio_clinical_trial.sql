--
-- Name: bio_clinical_trial; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_clinical_trial (
    trial_number character varying(510),
    study_owner character varying(510),
    study_phase character varying(100),
    blinding_procedure character varying(1000),
    studytype character varying(510),
    duration_of_study_weeks integer,
    number_of_patients integer,
    number_of_sites integer,
    route_of_administration character varying(510),
    dosing_regimen character varying(3500),
    group_assignment character varying(510),
    type_of_control character varying(510),
    completion_date timestamp without time zone,
    primary_end_points character varying(2000),
    secondary_end_points character varying(3500),
    inclusion_criteria text,
    exclusion_criteria text,
    subjects character varying(2000),
    gender_restriction_mfb character varying(510),
    min_age integer,
    max_age integer,
    secondary_ids character varying(510),
    bio_experiment_id bigint NOT NULL,
    development_partner character varying(100),
    geo_platform character varying(30),
    main_findings character varying(2000),
    platform_name character varying(200),
    search_area character varying(100)
);

--
-- Name: bio_clinical_trial clinicaltrialdim_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinical_trial
    ADD CONSTRAINT clinicaltrialdim_pk PRIMARY KEY (bio_experiment_id);

--
-- Name: bio_clinical_trial_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_clinical_trial_pk ON bio_clinical_trial USING btree (bio_experiment_id);

--
-- Name: bio_clinical_trial bio_clinical_trial_bio_experim; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_clinical_trial
    ADD CONSTRAINT bio_clinical_trial_bio_experim FOREIGN KEY (bio_experiment_id) REFERENCES bio_experiment(bio_experiment_id);

