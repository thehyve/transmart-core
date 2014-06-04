--
-- Name: antigen_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE antigen_deapp (
    antigen_id bigint NOT NULL,
    antigen_name character varying(100) NOT NULL,
    platform character varying(100) NOT NULL
);

--
-- Name: tf_trg_antigen_deapp(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_antigen_deapp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ANTIGEN_ID is null then
 select nextval('tm_cz.SEQ_ANTIGEN_ID') into NEW.ANTIGEN_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_antigen_deapp; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_antigen_deapp BEFORE INSERT ON antigen_deapp FOR EACH ROW EXECUTE PROCEDURE tf_trg_antigen_deapp();

--
-- Name: seq_antigen_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_antigen_id
    START WITH 46391
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

