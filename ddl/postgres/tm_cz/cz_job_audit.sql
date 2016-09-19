--
-- Name: cz_job_audit; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_job_audit (
    seq_id bigint NOT NULL,
    job_id bigint NOT NULL,
    database_name character varying(50),
    procedure_name character varying(100),
    step_desc character varying(1000),
    step_status character varying(50),
    records_manipulated bigint,
    step_number bigint,
    job_date timestamp without time zone,
    time_elapsed_secs double precision DEFAULT 0
);

--
-- Name: cz_job_audit_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_job_audit
    ADD CONSTRAINT cz_job_audit_pk PRIMARY KEY (seq_id);

--
-- Name: tf_trg_cz_seq_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_seq_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
      if NEW.SEQ_ID is null then
        select nextval('tm_cz.SEQ_CZ_JOB_AUDIT') into NEW.SEQ_ID ;       
      end if;       
       RETURN NEW;
  end;
$$;

--
-- Name: trg_cz_seq_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_seq_id BEFORE INSERT ON cz_job_audit FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_seq_id();

--
-- Name: seq_cz_job_audit; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_cz_job_audit
    START WITH 1686336
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 2;

