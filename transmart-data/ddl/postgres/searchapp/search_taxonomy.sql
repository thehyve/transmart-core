--
-- Name: search_taxonomy; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_taxonomy (
    term_id bigint NOT NULL,
    term_name character varying(900) NOT NULL,
    source_cd character varying(900),
    import_date timestamp(1) without time zone DEFAULT now(),
    search_keyword_id bigint
);

--
-- Name: search_taxonomy_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy
    ADD CONSTRAINT search_taxonomy_pk PRIMARY KEY (term_id);

--
-- Name: tf_trg_search_taxonomy_term_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_taxonomy_term_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.TERM_ID IS NULL THEN
		SELECT nextval('SEARCHAPP.SEQ_SEARCH_DATA_ID') INTO NEW.TERM_ID;
	END IF;
	RETURN NEW;
END;
$$;

--
-- Name: trg_search_taxonomy_term_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_taxonomy_term_id BEFORE INSERT ON search_taxonomy FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_taxonomy_term_id();

--
-- Name: fk_search_tax_search_keyword; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy
    ADD CONSTRAINT fk_search_tax_search_keyword FOREIGN KEY (search_keyword_id) REFERENCES search_keyword(search_keyword_id);

