--
-- Name: cz_write_info(numeric, numeric, numeric, character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION cz_write_info(jobid numeric, messageid numeric, messageline numeric, messageprocedure character varying, infomessage character varying) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

BEGIN
    begin
	insert into tm_cz.cz_job_message
    (
      job_id,
      message_id,
      message_line,
      message_procedure,
      info_message,
      seq_id
    )
	select
      jobID,
      messageID,
      messageLine,
      messageProcedure,
      infoMessage,
      max(seq_id)
  from
    tm_cz.cz_job_audit
  where
    job_id = jobID;
    end;
  
  COMMIT;
  return 1;

END;

$$;

