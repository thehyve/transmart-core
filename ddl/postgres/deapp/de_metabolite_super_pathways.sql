--
-- Name: de_metabolite_super_pathways; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_super_pathways (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    super_pathway_name character varying(200)
);

--
-- Name: de_metabolite_super_pathway_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_super_pathways
    ADD CONSTRAINT de_metabolite_super_pathway_pk PRIMARY KEY (id);

--
-- Name: de_metabolite_super_pathways_gpl_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_super_pathways
    ADD CONSTRAINT de_metabolite_super_pathways_gpl_id_fk FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform);

