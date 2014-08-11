--
-- Name: search_taxonomy_rels; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_taxonomy_rels (
    search_taxonomy_rels_id bigint NOT NULL,
    child_id bigint NOT NULL,
    parent_id bigint
);

--
-- Name: search_taxonomy_rels_pkey; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy_rels
    ADD CONSTRAINT search_taxonomy_rels_pkey PRIMARY KEY (search_taxonomy_rels_id);

--
-- Name: u_child_id_parent_id; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy_rels
    ADD CONSTRAINT u_child_id_parent_id UNIQUE (child_id, parent_id);

--
-- Name: tf_trg_search_taxonomy_rels_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_taxonomy_rels_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.SEARCH_TAXONOMY_RELS_ID IS NULL THEN
		SELECT nextval('SEARCHAPP.SEQ_SEARCH_DATA_ID') INTO NEW.SEARCH_TAXONOMY_RELS_ID;
	END IF;
	RETURN NEW;
end;
$$;

--
-- Name: trg_search_taxonomy_rels_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_taxonomy_rels_id BEFORE INSERT ON search_taxonomy_rels FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_taxonomy_rels_id();

--
-- Name: fk_search_tax_rels_child; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy_rels
    ADD CONSTRAINT fk_search_tax_rels_child FOREIGN KEY (child_id) REFERENCES search_taxonomy(term_id);

--
-- Name: fk_search_tax_rels_parent; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_taxonomy_rels
    ADD CONSTRAINT fk_search_tax_rels_parent FOREIGN KEY (parent_id) REFERENCES search_taxonomy(term_id);

