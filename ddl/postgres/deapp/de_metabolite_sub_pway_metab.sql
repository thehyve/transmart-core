--
-- Name: de_metabolite_sub_pway_metab; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_sub_pway_metab (
    metabolite_id bigint NOT NULL,
    sub_pathway_id bigint NOT NULL
);

--
-- Name: de_metabolite_sub_pway_metab_metabolite_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_sub_pway_metab
    ADD CONSTRAINT de_metabolite_sub_pway_metab_metabolite_id_fkey FOREIGN KEY (metabolite_id) REFERENCES de_metabolite_annotation(id);

--
-- Name: de_metabolite_sub_pway_metab_sub_pathway_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_sub_pway_metab
    ADD CONSTRAINT de_metabolite_sub_pway_metab_sub_pathway_id_fkey FOREIGN KEY (sub_pathway_id) REFERENCES de_metabolite_sub_pathways(id);

