--
-- Name: set_upload_status; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE set_upload_status (
    upload_id numeric NOT NULL,
    set_type_id integer NOT NULL,
    source_cd character varying(50) NOT NULL,
    no_of_record numeric,
    loaded_record numeric,
    deleted_record numeric,
    load_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone,
    load_status character varying(100),
    message text,
    input_file_name text,
    log_file_name text,
    transform_name character varying(500)
);

--
-- Name: pk_up_upstatus_idsettypeid; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY set_upload_status
    ADD CONSTRAINT pk_up_upstatus_idsettypeid PRIMARY KEY (upload_id, set_type_id);

--
-- Name: fk_up_set_type_id; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY set_upload_status
    ADD CONSTRAINT fk_up_set_type_id FOREIGN KEY (set_type_id) REFERENCES set_type(id);

