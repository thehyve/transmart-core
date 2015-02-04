--
-- Name: de_two_region_junction_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_two_region_junction_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_two_region_junction; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_two_region_junction (
    two_region_junction_id bigint DEFAULT nextval('de_two_region_junction_seq'::regclass) NOT NULL,
    up_end bigint NOT NULL,
    up_chr character varying(50) NOT NULL,
    up_pos bigint NOT NULL,
    up_strand character(1),
    down_end bigint NOT NULL,
    down_chr character varying(50) NOT NULL,
    down_pos bigint NOT NULL,
    down_strand character(1),
    is_in_frame boolean,
    external_id bigint,
    assay_id bigint
);

--
-- Name: COLUMN de_two_region_junction.up_end; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.up_end IS 'end of up stream junction';

--
-- Name: COLUMN de_two_region_junction.up_chr; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.up_chr IS 'chromosome of up stream fusion partner';

--
-- Name: COLUMN de_two_region_junction.up_pos; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.up_pos IS 'location of up stream fusion partner''s junction point';

--
-- Name: COLUMN de_two_region_junction.up_strand; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.up_strand IS 'strand of up stream junction, 1 for +, 0 for -';

--
-- Name: COLUMN de_two_region_junction.down_end; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.down_end IS 'end of down stream junction';

--
-- Name: COLUMN de_two_region_junction.down_chr; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.down_chr IS 'chromosome of down stream fusion partner';

--
-- Name: COLUMN de_two_region_junction.down_pos; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.down_pos IS 'location of down stream junction point';

--
-- Name: COLUMN de_two_region_junction.down_strand; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.down_strand IS 'strand of down stream junction, 1 for +, 0 for -';

--
-- Name: COLUMN de_two_region_junction.is_in_frame; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction.is_in_frame IS 'whether junction is frame-shift or in-frame-shift';

--
-- Name: de_two_region_junction_id_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_junction
    ADD CONSTRAINT de_two_region_junction_id_pk PRIMARY KEY (two_region_junction_id);

