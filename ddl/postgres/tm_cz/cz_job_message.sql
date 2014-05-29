--
-- Name: cz_job_message; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_job_message (
    job_id bigint NOT NULL,
    message_id bigint,
    message_line bigint,
    message_procedure character varying(100),
    info_message character varying(2000),
    seq_id bigint NOT NULL
);


--
-- Name: tf_trg_cz_message_seq_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_message_seq_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.SEQ_ID is null then
 select nextval('tm_cz.SEQ_CZ_JOB_MESSAGE') into NEW.SEQ_ID ;
endif;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_message_seq_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_message_seq_id BEFORE INSERT ON cz_job_message FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_message_seq_id();
