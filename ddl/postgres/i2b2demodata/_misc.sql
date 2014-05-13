--
-- Name: concept_id; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE concept_id
    START WITH 1288961
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: seq_patient_num; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE seq_patient_num
    START WITH 1000117527
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: sq_up_encdim_encounternum; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE sq_up_encdim_encounternum
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: ASYNC_JOB_SEQ
--
CREATE SEQUENCE async_job_seq
    MINVALUE 0
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 0
    CACHE 1
;

--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: PROTOCOL_ID_SEQ
--
CREATE SEQUENCE protocol_id_seq
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 215
    CACHE 1
;

--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: SEQ_SUBJECT_REFERENCE
--
CREATE SEQUENCE seq_subject_reference
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 743
    CACHE 1
;

