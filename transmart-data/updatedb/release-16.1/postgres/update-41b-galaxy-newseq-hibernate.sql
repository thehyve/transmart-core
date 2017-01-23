--
-- sequence galaxy.hibernate_id
--

set search_path = galaxy, pg_catalog;

--
-- Name: hibernate_id; Type: SEQUENCE; Schema: galaxy; Owner: -
--
CREATE SEQUENCE hibernate_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE galaxy.hibernate_id OWNER TO galaxy;
