--
-- Name: i2b2_tags; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2_tags (
    tag_id integer NOT NULL,
    path character varying(400),
    tag character varying(400),
    tag_type character varying(400),
    tags_idx integer NOT NULL
);


--
-- Name: tf_trg_i2b2_tag_id(); Type: FUNCTION; Schema: i2b2metadata; Owner: -
--
CREATE FUNCTION tf_trg_i2b2_tag_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TAG_ID is null then
 select nextval('i2b2metadata.SEQ_I2B2_DATA_ID') into NEW.TAG_ID ;
endif;
       RETURN NEW;
end;
$$;

--
-- Name: trg_i2b2_tag_id; Type: TRIGGER; Schema: i2b2metadata; Owner: -
--
CREATE TRIGGER trg_i2b2_tag_id BEFORE INSERT ON i2b2_tags FOR EACH ROW EXECUTE PROCEDURE tf_trg_i2b2_tag_id();
