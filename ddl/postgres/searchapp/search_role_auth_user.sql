--
-- Name: search_role_auth_user; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_role_auth_user (
    people_id bigint,
    authorities_id bigint
);

--
-- Name: fkfb14ef79287e0cac; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_role_auth_user
    ADD CONSTRAINT fkfb14ef79287e0cac FOREIGN KEY (authorities_id) REFERENCES search_auth_user(id);

--
-- Name: fkfb14ef798f01f561; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_role_auth_user
    ADD CONSTRAINT fkfb14ef798f01f561 FOREIGN KEY (people_id) REFERENCES search_role(id);

