--
-- Name: de_metabolite_sub_pway_metab; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_sub_pway_metab (
    metabolite_id bigint NOT NULL,
    sub_pathway_id bigint NOT NULL
);
--
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: SYS_C0010758
--
ALTER TABLE de_metabolite_sub_pway_metab ADD FOREIGN KEY (sub_pathway_id)
 REFERENCES de_metabolite_sub_pathways(id);
--
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: SYS_C0010759
--
ALTER TABLE de_metabolite_sub_pway_metab ADD FOREIGN KEY (metabolite_id)
 REFERENCES de_metabolite_annotation(id);
