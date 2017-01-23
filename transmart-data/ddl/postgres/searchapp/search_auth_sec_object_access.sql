--
-- Name: search_auth_sec_object_access; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_auth_sec_object_access (
    auth_sec_obj_access_id bigint NOT NULL,
    auth_principal_id bigint,
    secure_object_id bigint,
    secure_access_level_id bigint
);

--
-- Name: sch_sec_a_a_s_a_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_sec_object_access
    ADD CONSTRAINT sch_sec_a_a_s_a_pk PRIMARY KEY (auth_sec_obj_access_id);

--
-- Name: tf_trg_search_au_obj_access_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_au_obj_access_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.AUTH_SEC_OBJ_ACCESS_ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.AUTH_SEC_OBJ_ACCESS_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_search_au_obj_access_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_au_obj_access_id BEFORE INSERT ON search_auth_sec_object_access FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_au_obj_access_id();

--
-- Name: sch_sec_a_u_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_sec_object_access
    ADD CONSTRAINT sch_sec_a_u_fk FOREIGN KEY (auth_principal_id) REFERENCES search_auth_principal(id);

--
-- Name: sch_sec_s_a_l_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_sec_object_access
    ADD CONSTRAINT sch_sec_s_a_l_fk FOREIGN KEY (secure_access_level_id) REFERENCES search_sec_access_level(search_sec_access_level_id);

--
-- Name: sch_sec_s_o_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_sec_object_access
    ADD CONSTRAINT sch_sec_s_o_fk FOREIGN KEY (secure_object_id) REFERENCES search_secure_object(search_secure_object_id);

