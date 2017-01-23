--
-- Name: bio_asy_analysis_data_ext; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_data_ext (
    bio_asy_analysis_data_id bigint NOT NULL,
    ext_type character varying(20) NOT NULL,
    ext_data character varying(4000) NOT NULL
);

--
-- Name: bio_asy_analysis_data_ext_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_data_ext
    ADD CONSTRAINT bio_asy_analysis_data_ext_fk FOREIGN KEY (bio_asy_analysis_data_id) REFERENCES bio_assay_analysis_gwas(bio_asy_analysis_gwas_id);

