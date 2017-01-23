--
-- Name: timers; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE timers (
    timerid character varying(80) NOT NULL,
    targetid character varying(250) NOT NULL,
    initialdate timestamp(6) without time zone NOT NULL,
    timerinterval bigint,
    instancepk text,
    info text
);

--
-- Name: timers_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY timers
    ADD CONSTRAINT timers_pk PRIMARY KEY (timerid, targetid);

