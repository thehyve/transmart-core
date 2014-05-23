--
-- Name: de_snp_gene_map; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_gene_map (
    snp_id bigint,
    snp_name character varying(255),
    entrez_gene_id bigint
);

--
-- Name: fk_snp_gene_map_snp_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_gene_map
    ADD CONSTRAINT fk_snp_gene_map_snp_id FOREIGN KEY (snp_id) REFERENCES de_snp_info(snp_info_id);

