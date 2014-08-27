--
-- Name: search_sec_access_level; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_sec_access_level (
    search_sec_access_level_id bigint NOT NULL,
    access_level_name character varying(200),
    access_level_value bigint
);

--
-- Name: search_sec_ac_level_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_sec_access_level
    ADD CONSTRAINT search_sec_ac_level_pk PRIMARY KEY (search_sec_access_level_id);

--
-- Name: tf_trg_search_sec_acc_level_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_sec_acc_level_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.SEARCH_SEC_ACCESS_LEVEL_ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_SEC_ACCESS_LEVEL_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_sec_acc_level_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_sec_acc_level_id BEFORE INSERT ON search_sec_access_level FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_sec_acc_level_id();

