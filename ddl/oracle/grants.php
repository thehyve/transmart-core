<?php

$spec = [
    'BIOMART_USER' => [
        ['BIOMART',      '*TABLE',                    'READ'],
        ['BIOMART',      '*VIEW',                     'READ'],
        ['DEAPP',        '*TABLE',                    'READ'],
        ['DEAPP',        '*VIEW',                     'READ'],
        ['I2B2DEMODATA', '*TABLE',                    'READ'],
        ['I2B2DEMODATA', '*VIEW',                     'READ'],
        ['I2B2METADATA', '*TABLE',                    'READ'],
        ['I2B2METADATA', '*VIEW',                     'READ'],
        ['SEARCHAPP',    '*TABLE',                    'WRITE'],
        ['SEARCHAPP',    '*VIEW',                     'READ'],
		['I2B2DEMODATA', 'QT_QUERY_MASTER',           'WRITE'],
		['I2B2DEMODATA', 'QT_QUERY_INSTANCE',         'WRITE'],
		['I2B2DEMODATA', 'QT_QUERY_RESULT_INSTANCE',  'WRITE'],
		['I2B2DEMODATA', 'QT_PATIENT_SET_COLLECTION', 'WRITE'],
		['I2B2DEMODATA', 'ASYNC_JOB',                 'WRITE'],
		['DEAPP',        'DE_SAVED_COMPARISON',       'WRITE'],
		['BIOMART',      'BIO_ASSAY_FEATURE_GROUP',   'WRITE'],
		['BIOMART',      'BIO_MARKER',                'WRITE'],
		['BIOMART',      'BIO_ASSAY_DATA_ANNOTATION', 'WRITE'],
    ],
    'TM_CZ' => [
        ['BIOMART',      '*TABLE',             'FULL'],
        ['BIOMART',      '*VIEW',              'READ'],
		['BIOMART',      'TEA_NPV_PRECOMPUTE', 'EXECUTE'], /* I2B2_LOAD_OMICSOFT_DATA */
        ['DEAPP',        '*TABLE',             'FULL'],
		['DEAPP',        'SEQ_ASSAY_ID',       'READ'],    /* I2B2_PROCESS_MRNA_DATA */
        ['I2B2DEMODATA', '*TABLE',             'FULL'],
        ['I2B2DEMODATA', '*SEQUENCE',          'READ'],
        ['I2B2METADATA', '*TABLE',             'FULL'],
        ['I2B2METADATA', '*SEQUENCE',          'READ'],
        ['SEARCHAPP',    '*TABLE',             'FULL'],
        ['SEARCHAPP',    '*VIEW',              'READ'],
        ['TM_LZ',        '*TABLE',             'FULL'],
        ['TM_LZ',        '*VIEW',              'READ'],
        ['TM_WZ',        '*TABLE',             'FULL'],
        ['TM_WZ',        '*VIEW',              'READ'],
        ['FMAPP',        '*TABLE',             'FULL'],
        ['AMAPP',        '*TABLE',             'FULL'],
    ],
    'AMAPP' => [
        ['BIOMART', '*TABLE', 'READ'],
    ],
    'SEARCHAPP' => [
        ['BIOMART', 'BIO_ASSAY_DATA_ANNOTATION', 'READ'],
    ],
];
