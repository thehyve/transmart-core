CREATE TABLE searchapp.import_xnat_variable (
    id bigint NOT NULL,
    configuration_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    datatype character varying(255) NOT NULL,
    url character varying(255) NOT NULL
);

ALTER TABLE ONLY searchapp.import_xnat_variable
    ADD CONSTRAINT import_xnat_var_pk PRIMARY KEY (id);

CREATE OR REPLACE FUNCTION searchapp.tf_trg_import_xnat_var_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.ID ;
    end if;
RETURN NEW;
end;
$$;

CREATE TRIGGER trg_import_xnat_var_id BEFORE INSERT ON searchapp.import_xnat_variable FOR EACH ROW EXECUTE PROCEDURE searchapp.tf_trg_import_xnat_var_id();

ALTER TABLE ONLY searchapp.import_xnat_variable
    ADD CONSTRAINT import_xnat_var_fk FOREIGN KEY (configuration_id) REFERENCES searchapp.import_xnat_configuration(id);

GRANT DELETE, UPDATE, SELECT, INSERT ON TABLE searchapp.import_xnat_variable TO biomart_user;
GRANT ALL ON TABLE searchapp.import_xnat_variable TO searchapp;
GRANT ALL ON TABLE searchapp.import_xnat_variable TO tm_cz;