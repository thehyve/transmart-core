--
-- Name: news_updates; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE news_updates (
    newsid integer,
    ranbyuser character varying(200),
    rowsaffected integer,
    operation character varying(200),
    datasetname character varying(200),
    updatedate timestamp(6) without time zone,
    commentfield character varying(200)
);

