--
-- Name: bio_data_taxonomy; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_taxonomy (
    bio_taxonomy_id bigint NOT NULL,
    bio_data_id bigint NOT NULL,
    etl_source character varying(100)
);

--
-- Name: bio_taxon_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_taxonomy
    ADD CONSTRAINT bio_taxon_fk FOREIGN KEY (bio_taxonomy_id) REFERENCES bio_taxonomy(bio_taxonomy_id);

