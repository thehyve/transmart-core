--
-- Name: mesh_descriptor; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mesh_descriptor (
    mesh_descriptor_id character varying(15) NOT NULL,
    mesh_heading character varying(256),
    mesh_annotation character varying(2000)
);

