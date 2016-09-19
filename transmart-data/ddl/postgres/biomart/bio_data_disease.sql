--
-- Name: bio_data_disease; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_disease (
    bio_data_id bigint NOT NULL,
    bio_disease_id bigint NOT NULL,
    etl_source character varying(100)
);

--
-- Name: bio_data_disease_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_disease
    ADD CONSTRAINT bio_data_disease_pk PRIMARY KEY (bio_data_id, bio_disease_id);

--
-- Name: bio_dd_idx2; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_dd_idx2 ON bio_data_disease USING btree (bio_disease_id);

--
-- Name: bio_dt_dis_did_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_dt_dis_did_idx ON bio_data_disease USING btree (bio_data_id);

--
-- Name: bio_df_disease_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_disease
    ADD CONSTRAINT bio_df_disease_fk FOREIGN KEY (bio_disease_id) REFERENCES bio_disease(bio_disease_id);

