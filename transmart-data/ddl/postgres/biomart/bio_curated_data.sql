--
-- Name: bio_curated_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_curated_data (
    statement text,
    statement_status character varying(200),
    bio_data_id bigint NOT NULL,
    bio_curation_dataset_id bigint NOT NULL,
    reference_id bigint,
    data_type character varying(200)
);

--
-- Name: bio_curated_data bio_externalanalysis_fact_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_curated_data
    ADD CONSTRAINT bio_externalanalysis_fact_pk PRIMARY KEY (bio_data_id);

--
-- Name: bio_curated_data_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_curated_data_pk ON bio_curated_data USING btree (bio_data_id);

--
-- Name: bio_curated_data bio_ext_analys_ext_anl_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_curated_data
    ADD CONSTRAINT bio_ext_analys_ext_anl_fk FOREIGN KEY (bio_curation_dataset_id) REFERENCES bio_curation_dataset(bio_curation_dataset_id);

