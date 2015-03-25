--
-- Name: modifier_metadata; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE modifier_metadata (
    modifier_cd character varying(50) NOT NULL,
    valtype_cd character varying(10),
    std_units character varying(50),
    visit_ind character(1)
);

--
-- Name: modifier_metadata_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY modifier_metadata
    ADD CONSTRAINT modifier_metadata_pk PRIMARY KEY (modifier_cd);

--
-- Name: modifier_metadata_modifier_cd_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY modifier_metadata
    ADD CONSTRAINT modifier_metadata_modifier_cd_fk FOREIGN KEY (modifier_cd) REFERENCES modifier_dimension(modifier_cd) ON DELETE CASCADE;

