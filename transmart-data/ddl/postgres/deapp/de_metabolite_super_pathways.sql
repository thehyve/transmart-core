--
-- Name: de_metabolite_super_pathways; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_super_pathways (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    super_pathway_name character varying(200)
);

--
-- Name: de_metabolite_super_pathways de_metabolite_super_pathway_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_super_pathways
    ADD CONSTRAINT de_metabolite_super_pathway_pk PRIMARY KEY (id);

