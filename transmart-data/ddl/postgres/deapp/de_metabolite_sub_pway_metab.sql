--
-- Name: de_metabolite_sub_pway_metab; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_sub_pway_metab (
    metabolite_id bigint NOT NULL,
    sub_pathway_id bigint NOT NULL
);

--
-- Name: de_met_sub_pw_met_met_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_sub_pway_metab
    ADD CONSTRAINT de_met_sub_pw_met_met_id_fk FOREIGN KEY (metabolite_id) REFERENCES de_metabolite_annotation(id);

--
-- Name: de_met_sub_pw_met_sub_pw_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_sub_pway_metab
    ADD CONSTRAINT de_met_sub_pw_met_sub_pw_id_fk FOREIGN KEY (sub_pathway_id) REFERENCES de_metabolite_sub_pathways(id);

