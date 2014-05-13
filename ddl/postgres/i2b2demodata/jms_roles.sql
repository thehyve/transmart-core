--
-- Name: jms_roles; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE jms_roles (
    roleid character varying(32) NOT NULL,
    userid character varying(32) NOT NULL,
    PRIMARY KEY (userid, roleid)
);
