--
-- Name: de_rbm_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rbm_annotation (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    antigen_name character varying(200) NOT NULL,
    uniprot_id character varying(50),
    gene_symbol character varying(50),
    gene_id character varying(200),
    uniprot_name character varying(200)
);

--
-- Name: de_rbm_annotation_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_annotation
    ADD CONSTRAINT de_rbm_annotation_pkey PRIMARY KEY (id);

--
-- Name: tf_rbm_id_trigger(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_rbm_id_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ID is null then
 select nextval('deapp.RBM_ANNOTATION_ID') into NEW.ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: rbm_id_trigger; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER rbm_id_trigger BEFORE INSERT ON de_rbm_annotation FOR EACH ROW EXECUTE PROCEDURE tf_rbm_id_trigger();

--
-- Name: de_rbm_annotation_gpl_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rbm_annotation
    ADD CONSTRAINT de_rbm_annotation_gpl_id_fk FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform) ON DELETE CASCADE;

--
-- Name: rbm_annotation_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE rbm_annotation_id
    START WITH 105393
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

