--
-- Name: search_custom_filter; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_custom_filter (
    search_custom_filter_id bigint NOT NULL,
    search_user_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(2000),
    private character(1) DEFAULT 'N'::bpchar NOT NULL
);

--
-- Name: search_custom_filter_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_custom_filter
    ADD CONSTRAINT search_custom_filter_pk PRIMARY KEY (search_custom_filter_id);

--
-- Name: tf_trg_search_custom_filter_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_custom_filter_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin 
    if NEW.SEARCH_CUSTOM_FILTER_ID is null then
        select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_CUSTOM_FILTER_ID ;

    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_custom_filter_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_custom_filter_id BEFORE INSERT ON search_custom_filter FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_custom_filter_id();

