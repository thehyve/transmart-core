--
-- Type: SEQUENCE; Owner: I2B2METADATA; Name: SEQ_CONCEPT_CODE
--
CREATE SEQUENCE seq_concept_code
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 1000
    CACHE 1
;

--
-- Type: SEQUENCE; Owner: I2B2METADATA; Name: SEQ_I2B2_DATA_ID
--
CREATE SEQUENCE seq_i2b2_data_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 1789
    CACHE 1
;

--
-- Name: INDEX i2b2_c_comment_char_length_idx; Type: COMMENT; Schema: i2b2metadata; Owner: -
--
COMMENT ON INDEX i2b2_c_comment_char_length_idx IS 'For i2b2metadata.i2b2_trial_nodes view';

