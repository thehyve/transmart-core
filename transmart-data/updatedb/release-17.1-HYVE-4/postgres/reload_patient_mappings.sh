#!/bin/bash

pushd ../../../test_data
find studies/*/i2b2demodata/patient_mapping.tsv | groovy parse_and_reorder.groovy | groovy clean_postgres_tables.groovy
find studies/*/i2b2demodata/patient_mapping.tsv | groovy parse_and_reorder.groovy | groovy load_to_postgres.groovy
popd