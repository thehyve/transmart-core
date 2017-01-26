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

--
-- add documentation
--
COMMENT ON TABLE searchapp.search_secure_object IS 'Holds the secure tokens that control access.';

COMMENT ON COLUMN search_secure_object.search_secure_object_id IS 'Primary key. Id of the token.';
COMMENT ON COLUMN search_secure_object.bio_data_id IS 'Id of the data restricted by this token.';
COMMENT ON COLUMN search_secure_object.display_name IS 'The name used to display this token. E.g., Private Studies - SHARED_HD_CONCEPTS_STUDY_C_PR.';
COMMENT ON COLUMN search_secure_object.data_type IS 'Data type. E.g., BIO_CLINICAL_TRIAL.';
COMMENT ON COLUMN search_secure_object.bio_data_unique_id IS 'The unique id used to refer to the token. E.g., EXP:SHDCSCP.';
