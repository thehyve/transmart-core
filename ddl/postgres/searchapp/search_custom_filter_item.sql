--
-- Name: search_custom_filter_item; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_custom_filter_item (
    search_custom_filter_item_id bigint NOT NULL,
    search_custom_filter_id bigint NOT NULL,
    unique_id character varying(200) NOT NULL,
    bio_data_type character varying(100) NOT NULL
);

--
-- Name: search_cust_fil_item_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_custom_filter_item
    ADD CONSTRAINT search_cust_fil_item_pk PRIMARY KEY (search_custom_filter_item_id);

--
-- Name: tf_trg_search_cust_fil_item_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_cust_fil_item_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin 
    if NEW.SEARCH_CUSTOM_FILTER_ITEM_ID is null then
        select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_CUSTOM_FILTER_ITEM_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_cust_fil_item_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_cust_fil_item_id BEFORE INSERT ON search_custom_filter_item FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_cust_fil_item_id();

