--
-- Name: bio_recombination_rates; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_recombination_rates (
    chromosome character varying(20),
    "position" numeric(18,0),
    rate numeric(18,6),
    map numeric(18,6)
);

--
-- Name: idx_bio_recomb_1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_recomb_1 ON bio_recombination_rates USING btree ("position", chromosome);

