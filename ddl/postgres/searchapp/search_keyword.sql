--
-- Name: search_keyword; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_keyword (
    keyword character varying(400),
    bio_data_id bigint,
    unique_id character varying(500) NOT NULL,
    search_keyword_id bigint NOT NULL,
    data_category character varying(200) NOT NULL,
    source_code character varying(100),
    display_data_category character varying(200),
    owner_auth_user_id bigint
);

--
-- Name: search_keyword_uk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_keyword
    ADD CONSTRAINT search_keyword_uk UNIQUE (unique_id, data_category);

--
-- Name: search_kw_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_keyword
    ADD CONSTRAINT search_kw_pk PRIMARY KEY (search_keyword_id);

--
-- Name: search_keyword_index1; Type: INDEX; Schema: searchapp; Owner: -
--
CREATE INDEX search_keyword_index1 ON search_keyword USING btree (keyword);

--
-- Name: search_keyword_index2; Type: INDEX; Schema: searchapp; Owner: -
--
CREATE INDEX search_keyword_index2 ON search_keyword USING btree (bio_data_id);

--
-- Name: search_keyword_index3; Type: INDEX; Schema: searchapp; Owner: -
--
CREATE INDEX search_keyword_index3 ON search_keyword USING btree (owner_auth_user_id);

--
-- Name: tf_trg_search_keyword_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_keyword_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.SEARCH_KEYWORD_ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.SEARCH_KEYWORD_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_keyword_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_keyword_id BEFORE INSERT ON search_keyword FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_keyword_id();

