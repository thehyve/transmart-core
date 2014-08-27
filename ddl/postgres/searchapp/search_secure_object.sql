--
-- Name: search_secure_object; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_secure_object (
    search_secure_object_id bigint NOT NULL,
    bio_data_id bigint,
    display_name character varying(100),
    data_type character varying(200),
    bio_data_unique_id character varying(200)
);

--
-- Name: search_sec_obj_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_secure_object
    ADD CONSTRAINT search_sec_obj_pk PRIMARY KEY (search_secure_object_id);

--
-- Name: tf_trg_search_sec_obj_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_sec_obj_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.SEARCH_SECURE_OBJECT_ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_SECURE_OBJECT_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_sec_obj_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_sec_obj_id BEFORE INSERT ON search_secure_object FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_sec_obj_id();

