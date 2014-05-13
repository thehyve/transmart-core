--
-- Name: jms_users; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE jms_users (
    userid character varying(32) NOT NULL,
    passwd character varying(32) NOT NULL,
    clientid character varying(128),
    PRIMARY KEY (userid)
);
