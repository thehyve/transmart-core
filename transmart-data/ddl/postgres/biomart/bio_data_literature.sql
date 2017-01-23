--
-- Name: bio_data_literature; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_literature (
    bio_data_id bigint NOT NULL,
    bio_lit_ref_data_id bigint,
    bio_curation_dataset_id bigint NOT NULL,
    statement text,
    statement_status character varying(200),
    data_type character varying(200)
);

--
-- Name: bio_data_literature_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_literature
    ADD CONSTRAINT bio_data_literature_pk PRIMARY KEY (bio_data_id);

--
-- Name: bio_lit_curation_dataset_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_literature
    ADD CONSTRAINT bio_lit_curation_dataset_fk FOREIGN KEY (bio_curation_dataset_id) REFERENCES bio_curation_dataset(bio_curation_dataset_id);

