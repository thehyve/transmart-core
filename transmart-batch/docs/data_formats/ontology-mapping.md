Ontology mapping
================

The ontology mapping file is used to map variables declared in the [column mapping file](clinical.md)
to ontology codes and to contain the ancestry of the classes those codes represent.
The ontology codes will be used to create entries in the concept dimension.
The ancestry information will be used to create nodes in the ontology tree.


ONTOLOGY_MAP_FILE format
------------------------

|Category code|Data label   |Ontology code     |Label                                  |URI                                                    |Ancestors                            
|-------------|-------------|------------------|---------------------------------------|-------------------------------------------------------|-------------------------------------------------------
|Vital Signs  |Heart Rate   |SNOMEDCT/364075005|Heart rate                             |http://purl.bioontology.org/ontology/SNOMEDCT/364075005|SNOMEDCT/78564009,SNOMEDCT/364072008
|             |             |SNOMEDCT/78564009 |Pulse rate                             |http://purl.bioontology.org/ontology/SNOMEDCT/78564009 |SNOMEDCT/46680005,SNOMEDCT/310611001,SNOMEDCT/248627000
|             |             |SNOMEDCT/310611001|Cardiovascular measure                 |http://purl.bioontology.org/ontology/SNOMEDCT/310611001|SNOMEDCT/364066008
|             |             |SNOMEDCT/248627000|Pulse characteristics                  |http://purl.bioontology.org/ontology/SNOMEDCT/248627000|SNOMEDCT/364093006
|             |             |SNOMEDCT/364093006|Feature of peripheral pulse            |http://purl.bioontology.org/ontology/SNOMEDCT/364093006|SNOMEDCT/364089000
|             |             |SNOMEDCT/364089000|Systemic arterial feature              |http://purl.bioontology.org/ontology/SNOMEDCT/364089000|SNOMEDCT/364088008
|             |             |SNOMEDCT/364088008|Arterial feature                       |http://purl.bioontology.org/ontology/SNOMEDCT/364088008|SNOMEDCT/364087003
|             |             |SNOMEDCT/364087003|Blood vessel feature                   |http://purl.bioontology.org/ontology/SNOMEDCT/364087003|SNOMEDCT/364066008
|             |             |SNOMEDCT/364072008|Cardiac feature                        |http://purl.bioontology.org/ontology/SNOMEDCT/364072008|SNOMEDCT/364066008,SNOMEDCT/414236006
|             |             |SNOMEDCT/364066008|Cardiovascular observable              |http://purl.bioontology.org/ontology/SNOMEDCT/364066008|SNOMEDCT/363788007
|             |             |SNOMEDCT/414236006|Feature of anatomical entity           |http://purl.bioontology.org/ontology/SNOMEDCT/414236006|SNOMEDCT/414237002
|             |             |SNOMEDCT/414237002|Feature of entity                      |http://purl.bioontology.org/ontology/SNOMEDCT/414237002|SNOMEDCT/363787002
|             |             |SNOMEDCT/363788007|Clinical history/examination observable|http://purl.bioontology.org/ontology/SNOMEDCT/363788007|SNOMEDCT/363787002
|             |             |SNOMEDCT/46680005 |Vital signs                            |http://purl.bioontology.org/ontology/SNOMEDCT/46680005 |SNOMEDCT/363787002
|             |             |SNOMEDCT/363787002|Observable entity                      |http://purl.bioontology.org/ontology/SNOMEDCT/363787002|

Table, tab separated, txt file. It contains information about concepts and the ontology tree
that are uploaded to TranSMART.
The first two columns refer to columns in the [column mapping file](clinical.md).
The rows were these columns are not empty, are used for associating observations
with concept codes. The others are only used to build the tree.

Description of the columns:
- `Category Code`  The category name used in the column mapping file.
- `Data Label`  The variable name used in the column mapping file.
- `Ontology code`  Concept code to be used to classify observations.
- `Label`  Name of the concept.
- `URI`  Link to resource with metadata about the concept.
- `Ancestors`  Comma-separated list of ontology codes of classes of which this concept is a subclass.

An [example ontology mapping file](../../studies/TEST_17_1/clinical/OBS336-201_ontology_codes.txt) can be found in 
the [TEST_17_1](../../studies/TEST_17_1) example study.


Generating an ontology mapping file
-----------------------------------
An ontology mapping file can be generated using a `ontologymapping.params` file.

The following parameters are supported:
- `COLUMN_MAP_FILE`  (Example: `clinical/OBS336-201_columns.txt`)
- `ONTOLOGY_MAP_FILE` (Example: `ontology_codes.txt`)
- `ONTOLOGY_SERVICE_TYPE` (Example: `default`)

Two ontology types are currently implemented:
- `default`: a service that connects to the [mock-ontology-server](../../../mock-ontology-server) implementation.
- `bioontology`: a service that connects to the API of [bioontology.org](https://data.bioontology.org/documentation).

The following parameters are available for the `default` service type:
- `ONTOLOGY_SERVER_URL`: The URL of the ontology server.
- `ONTOLOGY_SERVER_SEARCH_PATH`:  The relative path used to search for recommended terms (Example: `/search`)
- `ONTOLOGY_SERVER_DETAILS_PATH`: The relative path used to lookup details of ontology terms (Example: `/codes/details`)

The following parameters are available for the `bioontology` service type:
- `ONTOLOGY_SERVER_URL`: The URL of the ontology server (Example: 'https://data.bioontology.org').
- `ONTOLOGY_SERVER_API_KEY`: The API key token, required to access the server. 
An API key can be obtained by registering at https://bioportal.bioontology.org/.
- `ONTOLOGY_SERVER_ONTOLOGIES`: The comma-separated list of ontologies to query (Example: `SNOMEDCT`).

An [example fetching parameters file](../../studies/TEST_17_1/ontologymapping.params) can be found in
the [TEST_17_1](../../studies/TEST_17_1) example study.

_Note_: The job tries to create the file specified in `ONTOLOGY_MAP_FILE` in the directory `ontologymapping`.
The job fails if the directory does not exist or if the specified file already exists.
If you want to rerun a previously completed job, you need to pass the `-n` options to transmart-batch.
