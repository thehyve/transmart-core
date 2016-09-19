--
-- Name: trg_concept_dimension_cd; Type: FUNCTION; Owner: i2b2demodata
--
  CREATE FUNCTION tf_trg_concept_dimension_cd() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	 if NEW.CONCEPT_CD is null then
	 select nextval('CONCEPT_ID') into NEW.CONCEPT_CD;
	 end if;
	 RETURN NEW;
	 end;

$$;

--
-- Name: trg_concept_dimension_cd; Type: TRIGGER; Schema: i2b2demodata; Owner: -
--
CREATE TRIGGER trg_concept_dimension_cd BEFORE INSERT ON concept_dimension FOR EACH ROW EXECUTE PROCEDURE tf_trg_concept_dimension_cd();
