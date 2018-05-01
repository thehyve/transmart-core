--
-- Name: patient_set_bitset; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.patient_set_bitset (
    result_instance_id NUMERIC(5,0) PRIMARY KEY,
    patient_set BIT VARYING NOT NULL
);