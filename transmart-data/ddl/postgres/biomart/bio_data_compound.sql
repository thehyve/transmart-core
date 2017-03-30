--
-- Name: bio_data_compound; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_compound (
    bio_data_id bigint NOT NULL,
    bio_compound_id bigint NOT NULL,
    etl_source character varying(100)
);

--
-- Name: bio_data_compound bio_data_compound_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_compound
    ADD CONSTRAINT bio_data_compound_pk PRIMARY KEY (bio_data_id, bio_compound_id);

--
-- Name: bio_data_compound bio_df_cmp_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_compound
    ADD CONSTRAINT bio_df_cmp_fk FOREIGN KEY (bio_compound_id) REFERENCES bio_compound(bio_compound_id);

