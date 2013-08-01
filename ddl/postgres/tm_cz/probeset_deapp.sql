--
-- Name: probeset_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE probeset_deapp (
    probeset_id bigint NOT NULL,
    probeset character varying(100) NOT NULL,
    platform character varying(100) NOT NULL,
    organism character varying(200)
);

--
-- Name: probeset_deapp_i1; Type: INDEX; Schema: tm_cz; Owner: -
--
CREATE INDEX probeset_deapp_i1 ON probeset_deapp USING btree (probeset_id);

--
-- Name: probeset_deapp_i2; Type: INDEX; Schema: tm_cz; Owner: -
--
CREATE INDEX probeset_deapp_i2 ON probeset_deapp USING btree (probeset, platform);

--
-- Name: tf_trg_probeset_deapp(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_probeset_deapp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
			if NEW.PROBESET_ID is null then
				select nextval('tm_cz.SEQ_PROBESET_ID') into NEW.PROBESET_ID ;       
			end if;   
	RETURN NEW;
end;
$$;

--
-- Name: trg_probeset_deapp; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_probeset_deapp BEFORE INSERT ON probeset_deapp FOR EACH ROW EXECUTE PROCEDURE tf_trg_probeset_deapp();

--
-- Name: seq_probeset_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_probeset_id
    START WITH 5461
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 99999999
    CACHE 20;

