--
-- Name: seq_data_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_data_id
    START WITH 11594011
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: seq_assay_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_assay_id
    START WITH 12973
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: seq_mrna_partition_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_mrna_partition_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Type: SEQUENCE; Owner: DEAPP; Name: METABOLITE_SUB_PTH_ID
--
CREATE SEQUENCE metabolite_sub_pth_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 7
    CACHE 1
;

--
-- Type: SEQUENCE; Owner: DEAPP; Name: METABOLITE_SUP_PTH_ID
--
CREATE SEQUENCE metabolite_sup_pth_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 5
    CACHE 1
;

--
-- Type: SEQUENCE; Owner: DEAPP; Name: METABOLOMICS_ANNOT_ID
--
CREATE SEQUENCE metabolomics_annot_id
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 8
    CACHE 1
;
