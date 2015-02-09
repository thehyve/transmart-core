--
-- Name: am_tag_value; Type: TABLE; Schema: amapp; Owner: -
--
CREATE TABLE am_tag_value (
    tag_value_id bigint NOT NULL,
    value character varying(2000)
);

--
-- Name: am_tag_value_pk; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_tag_value
    ADD CONSTRAINT am_tag_value_pk PRIMARY KEY (tag_value_id);

--
-- Name: tf_trg_am_tag_value_id(); Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE FUNCTION tf_trg_am_tag_value_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TAG_VALUE_ID is null then
 select nextval('amapp.SEQ_AMAPP_DATA_ID') into NEW.TAG_VALUE_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_am_tag_value_id; Type: TRIGGER; Schema: amapp; Owner: -
--
CREATE TRIGGER trg_am_tag_value_id BEFORE INSERT ON am_tag_value FOR EACH ROW EXECUTE PROCEDURE tf_trg_am_tag_value_id();

--
-- Name: tf_trg_am_tag_value_uid(); Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE FUNCTION tf_trg_am_tag_value_uid() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM amapp.am_data_uid 
  WHERE am_data_id = new.tag_value_id;
  
  if rec_count = 0 then
    insert into amapp.am_data_uid (am_data_id, unique_id, am_data_type)
    values (NEW.tag_value_id, am_tag_value_uid(NEW.tag_value_id), 'AM_TAG_VALUE');
  end if;
RETURN NEW;
end;
$$;


SET default_with_oids = false;

--
-- Name: trg_am_tag_value_uid; Type: TRIGGER; Schema: amapp; Owner: -
--
CREATE TRIGGER trg_am_tag_value_uid BEFORE INSERT ON am_tag_value FOR EACH ROW EXECUTE PROCEDURE tf_trg_am_tag_value_uid();

