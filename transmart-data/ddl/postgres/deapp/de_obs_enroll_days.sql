--
-- Name: de_obs_enroll_days; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_obs_enroll_days (
    encounter_num bigint,
    days_since_enroll double precision,
    study_id character varying(200)
);

--
-- Name: de_obs_enroll_days_idx2; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_obs_enroll_days_idx2 ON de_obs_enroll_days USING btree (study_id);

