--
-- Name: de_snp_probe_sort_def_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE de_snp_probe_sort_def_release (
    snp_probe_sorted_def_id bigint,
    platform_name character varying(255),
    num_probe bigint,
    chrom character varying(16),
    probe_def text,
    snp_id_def text
);

