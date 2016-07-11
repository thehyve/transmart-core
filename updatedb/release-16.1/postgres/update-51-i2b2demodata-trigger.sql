--
-- use local CONCEPT_ID sequence name in trigger
--

set search_path = i2b2demodata, pg_catalog;

CREATE OR REPLACE FUNCTION tf_trg_concept_dimension_cd() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	 if NEW.CONCEPT_CD is null then
	 select nextval('CONCEPT_ID') into NEW.CONCEPT_CD;
	 end if;
	 RETURN NEW;
	 end;

$$;
