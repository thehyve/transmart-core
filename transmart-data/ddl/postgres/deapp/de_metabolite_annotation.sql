--
-- Name: de_metabolite_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_metabolite_annotation (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    biochemical_name character varying(200) NOT NULL,
    biomarker_id character varying(200),
    hmdb_id character varying(50)
);

--
-- Name: de_metabolite_annotation de_metabolite_annotation_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_metabolite_annotation
    ADD CONSTRAINT de_metabolite_annotation_pk PRIMARY KEY (id);

