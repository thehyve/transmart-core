--
-- Name: am_tag_value; Type: TABLE; Schema: amapp; Owner: -
--
CREATE TABLE am_tag_value (
    tag_value_id bigint NOT NULL,
    value character varying(2000)
);
--
-- Name: tf_trg_am_tag_value_id; Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE FUNCTION tf_trg_am_tag_value_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TAG_VALUE_ID is null then
 select nextval('amapp.SEQ_AMAPP_DATA_ID') into NEW.TAG_VALUE_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_am_tag_value_id(); Type: TRIGGER; Schema: amapp; Owner: -
--
  CREATE TRIGGER trg_am_tag_value_id BEFORE INSERT ON am_tag_value FOR EACH ROW EXECUTE PROCEDURE tf_trg_am_tag_value_id();

--
-- Name: am_tag_value_pk; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_tag_value
    ADD CONSTRAINT am_tag_value_pk PRIMARY KEY (tag_value_id);
