--
-- Name: mirna_probeset_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE mirna_probeset_deapp (
    probeset_id bigint NOT NULL,
    probeset character varying(100),
    platform character varying(100),
    organism character varying(200)
);

--
-- Name: tf_trg_mirna_probeset_deapp; Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_mirna_probeset_deapp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.PROBESET_ID is null then
 select nextval('tm_cz.SEQ_PROBESET_ID') into NEW.PROBESET_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_mirna_probeset_deapp(); Type: TRIGGER; Schema: tm_cz; Owner: -
--
  CREATE TRIGGER trg_mirna_probeset_deapp BEFORE INSERT ON mirna_probeset_deapp FOR EACH ROW EXECUTE PROCEDURE tf_trg_mirna_probeset_deapp();

