--
-- Name: search_auth_group_member; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_auth_group_member (
    auth_user_id bigint,
    auth_group_id bigint
);

--
-- Name: sch_user_gp_m_grp_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_group_member
    ADD CONSTRAINT sch_user_gp_m_grp_fk FOREIGN KEY (auth_group_id) REFERENCES search_auth_group(id);

--
-- Name: sch_user_gp_m_usr_fk; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_group_member
    ADD CONSTRAINT sch_user_gp_m_usr_fk FOREIGN KEY (auth_user_id) REFERENCES search_auth_principal(id);

