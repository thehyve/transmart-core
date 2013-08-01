--
-- Name: de_xtrial_child_map; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_xtrial_child_map (
    concept_cd character varying(50) NOT NULL,
    parent_cd bigint NOT NULL,
    manually_mapped bigint,
    study_id character varying(50)
);

--
-- Name: sys_c0020605; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_xtrial_child_map
    ADD CONSTRAINT sys_c0020605 PRIMARY KEY (concept_cd);

