--
-- Name: saved_faceted_search; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE saved_faceted_search (
    saved_faceted_search_id bigint NOT NULL,
    user_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    keywords character varying(4000) NOT NULL,
    create_dt date DEFAULT now(),
    modified_dt date,
    search_type character varying(50) DEFAULT 'FACETED_SEARCH'::character varying NOT NULL,
    analysis_ids character varying(4000)
);

--
-- Name: saved_faceted_search_pkey; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY saved_faceted_search
    ADD CONSTRAINT saved_faceted_search_pkey PRIMARY KEY (saved_faceted_search_id);

--
-- Name: u_saved_search__user_id_name; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY saved_faceted_search
    ADD CONSTRAINT u_saved_search__user_id_name UNIQUE (user_id, name);

--
-- Name: tf_trg_saved_faceted_search_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_saved_faceted_search_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.saved_faceted_search_id IS NULL THEN
		SELECT nextval('SEARCHAPP.SEQ_saved_faceted_search_id') INTO NEW.saved_faceted_search_id;
	END IF;
	IF NEW.create_dt IS NULL THEN
		NEW.create_dt := now();
	END IF;
	RETURN NEW;
END;
$$;

--
-- Name: trg_saved_faceted_search_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_saved_faceted_search_id BEFORE INSERT ON saved_faceted_search FOR EACH ROW EXECUTE PROCEDURE tf_trg_saved_faceted_search_id();

--
-- Name: tf_trg_upd_saved_faceted_search(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_upd_saved_faceted_search() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.modified_dt IS NULL THEN
		NEW.modified_dt := now();
	END IF;
	RETURN NEW;
END;
$$;

--
-- Name: trg_upd_saved_faceted_search; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_upd_saved_faceted_search BEFORE UPDATE ON saved_faceted_search FOR EACH ROW EXECUTE PROCEDURE tf_trg_upd_saved_faceted_search();

--
-- Name: saved_faceted_search_user_id; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY saved_faceted_search
    ADD CONSTRAINT saved_faceted_search_user_id FOREIGN KEY (user_id) REFERENCES search_auth_user(id);

--
-- Name: seq_saved_faceted_search_id; Type: SEQUENCE; Schema: searchapp; Owner: -
--
CREATE SEQUENCE seq_saved_faceted_search_id
    START WITH 278
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

