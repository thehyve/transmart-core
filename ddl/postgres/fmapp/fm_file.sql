--
-- Name: fm_file; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_file (
    file_id bigint NOT NULL,
    display_name character varying(1000) NOT NULL,
    original_name character varying(1000) NOT NULL,
    file_version bigint,
    file_type character varying(100),
    file_size bigint,
    filestore_location character varying(1000),
    filestore_name character varying(1000),
    link_url character varying(1000),
    active_ind character(1) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone NOT NULL,
    PRIMARY KEY (file_id)
);
--
-- Name: tf_trg_fm_file_id; Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION tf_trg_fm_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.FILE_ID is null then
 select nextval('fmapp.SEQ_FM_ID') into NEW.FILE_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_fm_file_id(); Type: TRIGGER; Schema: fmapp; Owner: -
--
  CREATE TRIGGER trg_fm_file_id BEFORE INSERT ON fm_file FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_file_id();
 
--
-- Type: TRIGGER; Owner: FMAPP; Name: TRG_FM_FILE_UID
--

-- insert in table if UID is not already there

CREATE FUNCTION tf_trg_fm_file_uid() RETURNS trigger
LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM fm_data_uid 
  WHERE fm_data_id = new.file_id;
  
  if rec_count = 0 then
    insert into fmapp.fm_data_uid (fm_data_id, unique_id, fm_data_type)
    values (NEW.file_id, fm_file_uid(NEW.file_id), 'FM_FILE');
  end if;
end;
$$;

-- ALTER TRIGGER fmapp.trg_fm_file_uid;
CREATE TRIGGER trg_fm_file_uid AFTER INSERT ON fm_file FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_file_uid();
