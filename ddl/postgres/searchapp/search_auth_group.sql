--
-- Name: search_auth_group; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_auth_group (
    id bigint NOT NULL,
    group_category character varying(255)
);

--
-- Name: pk_auth_usr_group; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_group
    ADD CONSTRAINT pk_auth_usr_group PRIMARY KEY (id);

--
-- Name: sh_auth_gp_id_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_group
    ADD CONSTRAINT sh_auth_gp_id_fk FOREIGN KEY (id) REFERENCES search_auth_principal(id);

