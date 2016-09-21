--
-- Name: search_secure_object_path; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_secure_object_path (
    search_secure_object_id bigint,
    i2b2_concept_path character varying(2000),
    search_secure_obj_path_id bigint NOT NULL
);

--
-- Name: search_sec_obj__path_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_secure_object_path
    ADD CONSTRAINT search_sec_obj__path_pk PRIMARY KEY (search_secure_obj_path_id);

--
-- Name: tf_trg_search_sec_obj_path_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_sec_obj_path_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.SEARCH_SECURE_OBJ_PATH_ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_SECURE_OBJ_PATH_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_sec_obj_path_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_sec_obj_path_id BEFORE INSERT ON search_secure_object_path FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_sec_obj_path_id();

