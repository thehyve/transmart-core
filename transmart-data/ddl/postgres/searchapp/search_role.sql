--
-- Name: search_role; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_role (
    id bigint NOT NULL,
    version bigint,
    authority character varying(255),
    description character varying(255)
);

--
-- Name: sys_c0011120; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_role
    ADD CONSTRAINT sys_c0011120 PRIMARY KEY (id);

