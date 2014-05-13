--
-- Name: de_rbm_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rbm_annotation (
    id bigint,
    gpl_id character varying(50) NOT NULL,
    antigen_name character varying(200) NOT NULL,
    uniprot_id character varying(50),
    gene_symbol character varying(50),
    gene_id character varying(200),
    uniprot_name character varying(200),
    PRIMARY KEY (id)
);
--
-- Type: SEQUENCE; Owner: DEAPP; Name: RBM_ANNOTATION_ID
--
CREATE SEQUENCE rbm_annotation_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 12365
    CACHE 1
;

--
-- Name: tf_rbm_id_trigger; Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_rbm_id_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ID is null then
 select nextval('deapp.RBM_ANNOTATION_ID') into NEW.ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: rbm_id_trigger(); Type: TRIGGER; Schema: deapp; Owner: -
--
  CREATE TRIGGER rbm_id_trigger BEFORE INSERT ON de_rbm_annotation FOR EACH ROW EXECUTE PROCEDURE tf_rbm_id_trigger();
