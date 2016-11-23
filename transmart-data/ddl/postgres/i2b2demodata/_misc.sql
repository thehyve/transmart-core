--
-- Name: async_job_seq; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE async_job_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;

--
-- Name: protocol_id_seq; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE protocol_id_seq
    START WITH 215
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: seq_subject_reference; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE seq_subject_reference
    START WITH 743
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
-- Name: study_num_seq; Type: SEQUENCE OWNED BY; Schema: i2b2demodata; Owner: -
--
ALTER SEQUENCE study_num_seq
    OWNED BY study.study_num;

--
-- Name: trial_visit_num_seq; Type: SEQUENCE OWNED BY; Schema: i2b2demodata; Owner: -
--
ALTER SEQUENCE trial_visit_num_seq
OWNED BY trial_visit_dimension.trial_visit_num;
