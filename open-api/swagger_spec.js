var spec = {
  "swagger": "2.0",
  "info": {
    "version": 2.3,
    "title": "Transmart",
    "license": {
      "name": "Apache 2.0",
      "url": "http://www.apache.org/licenses/LICENSE-2.0.html"
    },
    "description": "\n# OAuth2\nAll calls need an Authorization header. https://wiki.transmartfoundation.org/display/transmartwiki/RESTful+API\n```\nAuthorization:Bearer {token}\n```\n\n# Constraints\nConstraints are used to build queries and are required in the `v2` API. They consist of a `Type` and that type's specific arguments. The implementation is in [Constraint.groovy](../transmart-core-db/src/main/groovy/org/transmartproject/db/multidimquery/query/Constraint.groovy).\n\n## Combinations (And/Or)\nMost often a combination of constraints is needed to get the right result. This can be done by the constraints with type \"and\" and \"or\".\nThey take a list `args` with constraints. All args will be evaluated together on each observation. So an 'and' operator with a `patient_set` and a `concept` will return all observations for the given concept linked to the patient set.\nHowever an `and` constraint with two ConceptConstraints will evaluate to an empty result, as no observation can have two concepts. This is also true even if nested with a different combination because constraints do not know scope.\n(There is also a constraint with type \"combination\" on which the And and Or constraints are built. It does not provide any functionality not provided by And and Or constraints, and it should be considered deprecated for direct usage.)\n\nExample:\n```json\n{\"type\": \"and\",\n \"args\": [\n    {\"type\": \"patient_set\", \"patientIds\": -62},\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}\n    ]\n}\n```\n\n```json\n{\"type\": \"or\",\n \"args\": [\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Blood Pressure\\\\\"}\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}\n    ]\n}\n```\n\n## StudyName\nEvaluate if an observation is part of a particular study\n\nExample:\n```json\n{\n  \"type\": \"study_name\",\n  \"studyId\": \"EHR\"\n}\n```\n\n## Concept\nEvaluate if an observation is of a particular Concept. Either by `path` or `conceptCode`.\n\n```json\n{\n  \"type\": \"concept\",\n  \"path\": \"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\",\n  \"conceptCode\": \"HR\"\n}\n```\n\n## Value\nEvaluate if the value of an observation is within the given parameters. It needs a `valueType`, `operator` and `value`.\n  `valueType`: [\\\"NUMERIC\\\", \\\"STRING\\\"]\n  `operator`: [&lt;, &gt;, =, !=, &lt;=, &gt;=, in, like, contains]\n\nExample:\n```json\n{\n  \"type\": \"value\",\n  \"valueType\": \"NUMERIC\",\n  \"operator\": \"=\", \"value\": 176\n}\n```\n\n## Field\nEvaluate if a specific field of an observation is within the given parameters. it needs a `field`, `operator` and `value`.\n  `operator`: [&lt;, &gt;, =, !=, &lt;=, &gt;=, in, like, contains]\n\nExample:\n```json\n{\n  \"type\": \"field\",\n  \"field\": {\n      \"dimension\": \"patient\",\n      \"fieldName\": \"age\",\n      \"type\": \"NUMERIC\"\n      },\n  \"operator\": \"!=\",\n  \"value\": 100\n}\n```\n\n## Time\nEvaluate if an observation is within the specified time period. It needs a `field` the type of which needs to be `DATE`. It needs a time relevant `operator` and a list of `values`.\nThe list must hold one date for the before(<-) and after(->) operator. It must hold two dates for the between(<-->) operator. If the given date field for an observation is empty, the observation will be ignored.\n`operator`: [\"&lt;-\", \"-&gt;\", \"&lt;--&gt;\"]\n\nExample:\n```json\n{\n  \"type\": \"time\",\n  \"field\": {\n      \"dimension\": \"start time\",\n      \"fieldName\": \"startDate\",\n      \"type\": \"DATE\"\n      },\n  \"operator\": \"->\",\n  \"values\": [\"2016-01-01T00:00:00Z\"]\n}\n```\n\n## PatientSet\nEvaluate if an observation is liked to a patient in the set. You have to provide one of three: a `patientSetId`, a list of `patientIds` or a list of `subjectIds`.\n\nExamples:\nBy specifying a patient set id.\n```json\n{\n    \"type\": \"patient_set\",\n    \"patientSetId\": 28820\n}\n```\nBy specifying a list of patient identifiers.\n```json\n{\n    \"type\": \"patient_set\",\n    \"patientIds\": [-62, -63]\n}\n```\nAnd by specifying a list of subject identifiers (aka external identifiers).\n```json\n{\n    \"type\": \"patient_set\",\n    \"subjectIds\": [\"4543AB\", \"4543AC\"]\n}\n```\n\n## SubSelection\nCreate a subselection of patients, visits, or another dimension element, and then select all observations for these dimension elements.\n\nExample: Select all observations for patients who have a certain diagnosis.\n```json\n{\n    \"type\": \"subselection\",\n    \"dimension\": \"patient\",\n    \"constraint\": {\n        \"type\": \"and\",\n        \"args\": [{\n                \"type\": \"concept\",\n                \"path\": \"\\\\Public Studies\\\\EHR\\\\Diagnosis\\\\\",\n                \"conceptCode\": \"DIAG\"\n            }, {\n                \"type\": \"value\",\n                \"valueType\": \"STRING\",\n                \"operator\": \"=\",\n                \"value\": \"My eye hurts\"\n            }]\n    }\n}\n```\n\n## Temporal\nEvaluate if an observation happened before or after an event. It needs an `operator` and an `event`. Any constraint can be used as an event. Most likely a combination.\n`operator`: [\"&lt;-\", \"-&gt;\", \"exists\"]\n\nExample:\n```json\n{\n    \"type\": \"temporal\",\n    \"operator\": \"exists\",\n    \"event\": {\n          \"type\": \"value\",\n          \"valueType\": \"NUMERIC\",\n          \"operator\": \"=\",\n          \"value\": 60\n          }\n}\n```\n\n## Null\nEvaluate if an specific field of an observation is null. It needs a field.\n\nExample:\n```json\n{\n    \"type\": \"null\",\n    \"field\":{\n        \"dimension\": \"end time\",\n        \"fieldName\": \"endDate\",\n        \"type\": \"DATE\"\n        }\n}\n```\n\n## Modifier\nEvaluate if an observation is linked to the specified modifier. Optionally if that modifier has the specific value. It must have a `path`, `dimensionName` or `modifierCode` and may have `values` in the form of a ValueConstraint.\n\nExample:\n```json\n{\n    \"type\": \"modifier\",\n    \"modifierCode\": \"TNS:SMPL\",\n    \"path\": \"\\\\Public Studies\\\\TUMOR_NORMAL_SAMPLES\\\\Sample Type\\\\\",\n    \"dimensionName\": \"sample_type\",\n    \"values\": {\n        \"type\": \"value\",\n        \"valueType\": \"STRING\",\n        \"operator\": \"=\",\n        \"value\": \"Tumor\"\n        }\n}\n```\n\n## Negation\nEvaluate if for an observation the given `arg` is false. `arg` is a constraint.\n\nExample:\n```json\n{\n    \"type\": \"negation\",\n    \"arg\": {\n        \"type\": \"patient_set\",\n        \"patientIds\": [-62,-52,-42]\n        }\n}\n```\nreturns all observations not linked to patient with id -62, -52 or -42\n\n## Biomarker\nUsed to filter high dimensional observations. It needs a 'biomarkerType' and a 'params' object. It can only be used\nwhen retrieving high dimensional data, and if so needs to be specified in a separate url parameter\n`biomarker_constraint`.\n`biomarkerType`: `[\"transcripts\", \"genes\"]`.\n\nExample:\n```json\n{\n    \"type\": \"biomarker\",\n    \"biomarkerType\": \"genes\",\n    \"params\": {\n        \"names\": [\"TP53\"]\n        }\n}\n```\n\n## True\n**!!WARNING!!** Use mainly for testing.  \nThe most basic of constraints. Evaluates to true for all observations. This returns all observations the requesting user has access to.\n\nExample:\n```json\n{\n    \"type\": \"true\"\n}\n```\n\n## Relation\nPedigree relations constraints.\n`relationTypeLabel` is mandatory and specifies code of relation. e.g. PAR (Parent-Child)\n`relatedSubjectsConstraint` is optional and specifies a constraint to apply to the related subjects.\n`biological` is optional field to restrict whether relation biological or not. If not specified, no filtering on this flag applied.\n`shareHousehold` is optional field to restrict whether subjects share the same address or not. If not specified, no filtering on this flag applied.\n\nBelow is example of selecting subjects that are parents (`relationTypeLabel=PAR`) of subjects with ids `-62`, `-52` and `-42`.\n```json\n{\n    \"type\": \"relation\",\n    \"relatedSubjectsConstraint\": {\n       \"type\": \"patient_set\",\n       \"patientIds\": [-62,-52,-42]\n    }\n    \"relationTypeLabel\": \"PAR\",\n    \"biological\": true,\n    \"shareHousehold\": true\n}\n```\n\n# Response types\n#### application/json\nAll calls support json. however this might not always be the best option. You will find schemas for the responses in this documentation.\n\n#### `application/hal+json`\nOnly the tree_node endpoint supports the application/hal+json format.\n\n#### `application/x-protobuf`\nCalls that return observations support protobuf as a more efficient binary format. The description of the protobuf object can be found in [observations.proto](../transmart-rest-api/src/protobuf/v2/observations.proto). Information on [google protobuf](https://developers.google.com/protocol-buffers/).\n"
  },
  "schemes": [
    "http",
    "https"
  ],
  "consumes": [
    "application/json"
  ],
  "produces": [
    "application/json"
  ],
  "securityDefinitions": {
    "oauth": {
      "type": "oauth2",
      "flow": "implicit",
      "authorizationUrl": "/oauth/authorize",
      "scopes": {
        "basic": "to be able to interact with transmart REST-API"
      }
    }
  },
  "security": [
    {
      "oauth": [
        "basic"
      ]
    }
  ],
  "paths": {
    "/versions": {
      "get": {
        "description": "Gets all available API versions and prefixes. The API version is separate from the version of transmart itself.\n\nThe API version follows semantic versioning: major versions can be incompatible, but minor version upgrades should be compatible with previous versions within the same major version. Patch versions are not used at the moment. The rest api can support multiple major versions at the same time using different prefixes.\n\nThe default settings expose this endpoint without any authentication requirements, as the version info may be needed to select an authentication method, however clients should be prepared to only be able to see the supported major versions without authentication. In that case the innermost dictionary in the response json will only contain \"id\", \"prefix\" and \"major\" keys.\n\nDevelopment and preview releases may also contain version tags, e.g. the version leading up to the development of 2.1 can be called 2.1-dev. Such tagged releases also support separate feature revisions. See `transmart-rest-api/grails-app/controllers/org/transmartproject/rest/VersionController.groovy` for details about that.\n",
        "responses": {
          "200": {
            "description": "Successful response. Example:\n`{ \"versions\": {\n    \"v2\": {\n        \"id\": \"v2\",\n        \"prefix\": \"/v2\",\n        \"version: \"2.1\",\n        \"major\": 2,\n        \"minor\": 1\n    }\n} }`'\n",
            "schema": {
              "type": "object",
              "properties": {
                "versions": {
                  "type": "object",
                  "properties": {
                    "version ids": {
                      "$ref": "#/definitions/Version"
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "/versions/{id}": {
      "get": {
        "description": "Gets information about the version if available. This returns the same information as `/versions`, but only for a single version.\n",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "description": "id of the version to fetch. Example: `GET /versions/v1`.\n",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful response.",
            "schema": {
              "$ref": "#/definitions/Version"
            }
          },
          "404": {
            "description": "Version not available."
          }
        }
      }
    },
    "/v1/studies": {
      "get": {
        "description": "Gets all `Study` objects.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "object",
              "properties": {
                "studies": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/jsonStudy"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v1/studies (hal+json)": {
      "get": {
        "description": "Gets all `Study` objects.\n",
        "produces": [
          "application/hal+json"
        ],
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "$ref": "#/definitions/HalStudies"
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}": {
      "get": {
        "description": "Gets a `Study` objects.\n",
        "tags": [
          "v1"
        ],
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "studyid to fetch",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "$ref": "#/definitions/JsonStudy"
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/concepts": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets all `concepts`  for a study.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "object",
              "properties": {
                "ontology_terms": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/OntologyTerm"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/concepts/{conceptPath}": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          },
          {
            "name": "conceptPath",
            "in": "path",
            "description": "Concept path for which info will be fetched",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets a `concept` objects.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "$ref": "#/definitions/OntologyTerm"
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/subjects": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets all `subjects` for a study.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "object",
              "properties": {
                "subjects": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Patient"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/subjects/{subjectid}": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          },
          {
            "name": "subjectid",
            "in": "path",
            "description": "Subject ID of the subject which will be fetched",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets a `subject` objects.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "$ref": "#/definitions/Patient"
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/concepts/{conceptPath}/subjects": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          },
          {
            "name": "conceptPath",
            "in": "path",
            "description": "Concept path for which info will be fetched",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets all `subjects` for a given study and concept.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "object",
              "properties": {
                "subjects": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Patient"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v1/studies/{studyid}/observations": {
      "get": {
        "parameters": [
          {
            "name": "studyid",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched.",
            "required": true,
            "type": "string"
          }
        ],
        "description": "Gets all `observations` for a study.\n",
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/LegacyObservation"
              }
            }
          }
        }
      }
    },
    "/v1/observations": {
      "get": {
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/LegacyObservation"
              }
            }
          }
        }
      }
    },
    "/v1/studies/{studyId}/concepts/{conceptPath}/observations": {
      "get": {
        "parameters": [
          {
            "name": "studyId",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          },
          {
            "name": "conceptPath",
            "in": "path",
            "description": "Concept path",
            "required": true,
            "type": "string"
          }
        ],
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/LegacyObservation"
              }
            }
          }
        }
      }
    },
    "/v1/patient_sets/": {
      "post": {
        "parameters": [
          {
            "name": "i2b2query_xml",
            "in": "body",
            "description": "Body should be a query definition in a subset of the i2b2 XML schema.",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "tags": [
          "v1"
        ],
        "responses": {
          "201": {
            "description": "Successful response"
          }
        }
      },
      "get": {
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successfull response"
          }
        }
      }
    },
    "/v1/patient_sets/{resultInstanceId}": {
      "get": {
        "parameters": [
          {
            "name": "resultInstanceId",
            "in": "path",
            "description": "ID of the patient set, called resultInstance ID because internally it refers to the result of a query",
            "required": true,
            "type": "string"
          }
        ],
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successfull response"
          }
        }
      }
    },
    "/v1/studies/{studyId}/concepts/{conceptPath}/highdim": {
      "get": {
        "parameters": [
          {
            "name": "studyId",
            "in": "path",
            "description": "Study ID of the study for which concepts will be fetched",
            "required": true,
            "type": "string"
          },
          {
            "name": "conceptPath",
            "in": "path",
            "description": "Concept path",
            "required": true,
            "type": "string"
          },
          {
            "name": "dataType",
            "in": "query",
            "description": "Data Type constraint",
            "required": false,
            "type": "string"
          },
          {
            "name": "projection",
            "in": "query",
            "description": "Projection applied to the HDD",
            "required": false,
            "type": "string"
          },
          {
            "name": "assayConstraints",
            "in": "query",
            "description": "Assay Constraints",
            "required": false,
            "type": "string"
          },
          {
            "name": "dataConstraints",
            "in": "query",
            "description": "Data constraint",
            "required": false,
            "type": "string"
          }
        ],
        "tags": [
          "v1"
        ],
        "responses": {
          "200": {
            "description": "Successful response"
          }
        }
      }
    },
    "/v2/studies": {
      "get": {
        "description": "Gets all studies accessible by the user.\n",
        "tags": [
          "v2"
        ],
        "produces": [
          "application/json"
        ],
        "responses": {
          "200": {
            "description": "Returns a list of studies\n",
            "schema": {
              "type": "object",
              "properties": {
                "studies": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Study"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/studies/{id}": {
      "get": {
        "description": "Gets the study with id `id`.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "description": "id of the study to fetch",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns one study\n",
            "schema": {
              "$ref": "#/definitions/Study"
            }
          },
          "404": {
            "description": "Study not found or no access\n"
          }
        }
      }
    },
    "/v2/studies/studyId/{studyId}": {
      "get": {
        "description": "Gets the study with study id `studyId`.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "studyId",
            "in": "path",
            "description": "the study id of the study to fetch",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns one study\n",
            "schema": {
              "$ref": "#/definitions/Study"
            }
          },
          "404": {
            "description": "Study not found or no access\n"
          }
        }
      }
    },
    "/v2/studies/studyIds": {
      "get": {
        "description": "Gets the studies with study ids `studyIds`. Deprecated: use `/v2/studies` and `/v2/studies/studyId/{studyId}` instead.\n",
        "deprecated": true,
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "studyIds",
            "in": "query",
            "description": "Json list of strings, with each string being a study name. Example: `/v2/studies/studyIds?studyIds=[\"GSE8581\", \"EHR\"]`",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns a list of studies\n",
            "schema": {
              "type": "object",
              "properties": {
                "studies": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Study"
                  }
                }
              }
            }
          },
          "404": {
            "description": "Study not found or no access\n"
          }
        }
      }
    },
    "/v2/supported_fields": {
      "get": {
        "description": "Gets all supported dimension fields. These are the fields that can be used in field constraints.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "Returns a list of supported fields\n",
            "schema": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "dimension": {
                    "type": "string"
                  },
                  "fieldName": {
                    "type": "string"
                  },
                  "type": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations": {
      "get": {
        "description": "Gets all observations that satisfy the given constaint. Only observations the calling user has access to are returned. Empty and null values are returned but have no value property\n",
        "tags": [
          "v2"
        ],
        "produces": [
          "application/json",
          "application/x-protobuf"
        ],
        "parameters": [
          {
            "name": "type",
            "required": true,
            "in": "query",
            "description": "specifies the type of the data you want to retrieve. For clinical data specify `clinical`, for high dimensional data specify the data type or use `autodetect`. If you use `autodetect` the constraints must be such that only a single type of high dimensional data matches.",
            "type": "string"
          },
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"study_name\", \"studyId\":\"EHR\"}` or `{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\CLINICAL_TRIAL_HIGHDIM\\\\High Dimensional data\\\\Expression Lung\\\\\"}`.",
            "type": "string"
          },
          {
            "name": "sort",
            "required": false,
            "in": "query",
            "type": "string",
            "description": "json string that specifies the sort order of the observations. Sorting can be done on multiple dimensions. `[\"patient\"]` sorts the observations on the patient dimension, `[\"patient\", \"concept\"]` sorts the observations first on the patient, and then on concept. The sort order is on the 'key' field of the chosen dimension, sorting on arbitrary fields is not yet supported. For patients, this is the `id`, for concepts the `conceptCode`.\nIt is also possible to specify the sort order to be ascending or descending. Use `[['patient', 'asc'], ['concept', 'desc']]` for that to sort the observations on patients first, ascending, and then on concepts descending.\nWhen requesting modifier dimensions, the supported sortings is very limited due to implementation constraints. Sorting support is limited to those dimensions that make up the primary key columns in the `i2b2demodata.observation_fact` table, and a few other supported dimensions. If you request a sort order that is not supported you will receive an HTTP 400 Bad Request error code.\n"
          },
          {
            "name": "biomarker_constraint",
            "required": false,
            "in": "query",
            "description": "json that describes the biomarker. The only valid type is the 'biomarker' constraint Example: `{\"type\":\"biomarker\", \"biomarkerType\":\"genes\",\"params\":{\"names\":[\"TP53\"]}}`.",
            "type": "string"
          },
          {
            "name": "projection",
            "required": false,
            "in": "query",
            "description": "The projection. Only used with high dimensional data Example: `all_data`, `zscore`, `log_intensity`. Default: `all_data`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Dimensions are described in the `header`. The order in which they appear in the header, determines the order in which they appear in the `cells` and footer. The value in the `dimensionIndexes` corresponds to the values in the `footer`.\n",
            "schema": {
              "$ref": "#/definitions/Observations"
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "produces": [
          "application/json",
          "application/x-protobuf"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "description": "The parameters",
            "schema": {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "description": "see GET parameters"
                },
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                },
                "biomarker_constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                },
                "projection": {
                  "type": "string",
                  "description": "see GET parameters"
                },
                "sort": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "type",
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Dimensions are described in the `header`. The order in which they appear in the header, determines the order in which they appear in the `cells` and footer. The value in the `dimensionIndexes` corresponds to the values in the `footer`.\n",
            "schema": {
              "$ref": "#/definitions/Observations"
            }
          }
        }
      }
    },
    "/v2/observations/aggregates_per_concept": {
      "get": {
        "description": "Calculates and returns an aggregates for both types of concepts, categorical and numerical. Deprecated. Use POST instead.\n",
        "deprecated": true,
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "Constraint specification. Example: `{\"type\":\"concept\",\"conceptCode\":\"EHR:VSIGN:HR\"}`.'\n",
            "type": "object"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns the result as a map from concept code to aggregates.\nExample: `{\"aggregatesPerConcept\":{\"EHR:VSIGN:HR\": {\"numericalValueAggregates\":{\"min\":56,\"max\":102,\"count\":9,\"avg\":74.78,\"stdDev\":14.7}}}}`.'\n",
            "schema": {
              "type": "object",
              "properties": {
                "aggregatesPerConcept": {
                  "$ref": "#/definitions/AggregatesMap"
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Calculates and returns an aggregates for both types of concepts, categorical and numerical.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "object",
                  "required": true,
                  "description": "Constraint specification. Example: `{\"type\":\"concept\",\"conceptCode\":\"EHR:VSIGN:HR\"}`.'\n"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Returns the result as a map from concept code to aggregates.\nExample: `{\"aggregatesPerConcept\":{\"EHR:VSIGN:HR\": {\"numericalValueAggregates\":{\"min\":56,\"max\":102,\"count\":9,\"avg\":74.78,\"stdDev\":14.7}}}}`.'\n",
            "schema": {
              "type": "object",
              "properties": {
                "aggregatesPerConcept": {
                  "$ref": "#/definitions/AggregatesMap"
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations/counts": {
      "get": {
        "description": "Counts the number of observations that satisfy the given constraint and the number of associated patients.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Return the result as a json object. Example: `{observationCount: 56, patientCount: 12}`.",
            "schema": {
              "$ref": "#/definitions/Counts"
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Return the result as a json object. Example: `{observationCount: 56, patientCount: 12}`.",
            "schema": {
              "$ref": "#/definitions/Counts"
            }
          }
        }
      }
    },
    "/v2/observations/counts_per_study": {
      "get": {
        "description": "Counts the number of observations and patients that satisfy the given constraint and groups them by study.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from study id to counts. Example: `{countsPerStudy: {SHARED_A: 2, SHARED_B: 3}}`.",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerStudy": {
                  "$ref": "#/definitions/CountsMap"
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from study id to counts. Example: `{countsPerStudy: {SHARED_A: 2, SHARED_B: 3}}`.",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerStudy": {
                  "$ref": "#/definitions/CountsMap"
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations/counts_per_concept": {
      "get": {
        "description": "Counts the number of observations and patients that satisfy the given constraint and groups them by concept.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from concept code to counts. Example: `{countsPerConcept: {\"DEM:AGE\": 2, \"HR\": 3}}`.",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerConcept": {
                  "$ref": "#/definitions/CountsMap"
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from concept code to counts. Example: `{countsPerConcept: {\"DEM:AGE\": 2, \"HR\": 3}}`.",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerConcept": {
                  "$ref": "#/definitions/CountsMap"
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations/counts_per_study_and_concept": {
      "get": {
        "description": "Counts the number of observations and patients that satisfy the given constraint and groups them by study and concept.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from study id to a map from concept code to counts.\nExample: `{countsPerStudy: {\"SHARED_A\": {\"DEM:AGE\": 2, \"HR\": 3}, \"SHARED_B\": {\"DEM:AGE\": 1}}}`.\n",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerStudy": {
                  "$ref": "#/definitions/CountsMapMap"
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "The result as a json object with a map from study id to a map from concept code to counts.\nExample: `{countsPerStudy: {\"SHARED_A\": {\"DEM:AGE\": 2, \"HR\": 3}, \"SHARED_B\": {\"DEM:AGE\": 1}}}`.\n",
            "schema": {
              "type": "object",
              "properties": {
                "countsPerStudy": {
                  "$ref": "#/definitions/CountsMapMap"
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations/table": {
      "post": {
        "description": "Returns a tabular view of observations, as a table using specified dimensions for rows and columns\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "description": "Data table parameters",
            "schema": {
              "type": "object",
              "properties": {
                "type": {
                  "required": true,
                  "type": "string",
                  "description": "The type of data to retrieve. At the moment only 'clinical' is supported. High dimensional\ndata is not supported.\n"
                },
                "constraint": {
                  "required": true,
                  "type": "object",
                  "description": "Constraint specification. Example: `{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital\nSigns\\\\Heart Rate\\\\\"}`.\n"
                },
                "rowDimensions": {
                  "required": true,
                  "type": "array",
                  "items": {
                    "type": "string"
                  },
                  "description": "List of strings, with each string being a dimension name. This specifies the dimensions\nthat form the vertical axis of the table (in order). Example: `['study', 'patient']`\n"
                },
                "columnDimensions": {
                  "required": true,
                  "type": "array",
                  "items": {
                    "type": "string"
                  },
                  "description": "The same as `rowDimensions`, but with the dimensions that specify the horizontal axis of the table"
                },
                "rowSort": {
                  "required": false,
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/SortSpecification"
                  },
                  "description": "List of sort specifications for the row dimensions. A sort specification is\nan element `{dimension: <dimension>, sortOrder: <direction>}` where `<dimension>` is the name of a\ndimension and `<direction>` is either `'asc'` or `'desc'`. The dimension must be a dimension that is\npart of the `rowDimensions` parameter. This defaults to the order specified in `rowDimensions`.\nExample: `[{dimension: 'study', sortOrder: 'desc'}, {dimension: 'patient', sortOrder:'asc'}]`\n"
                },
                "columnSort": {
                  "required": false,
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/SortSpecification"
                  },
                  "description": "Analogous to `rowSort`, but for the column dimensions."
                },
                "limit": {
                  "required": true,
                  "type": "integer",
                  "description": "The maximum number of rows to return. This determines the size of the result."
                },
                "offset": {
                  "required": false,
                  "type": "integer",
                  "description": "The number of rows to skip before the first returned row. If the dataset does not contain at least\nlimit+offset number of rows, the returned result will contain the `limit` last rows of the dataset. (The\nresult also includes the actual offset of the returned rows.)\n"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "$ref": "#/definitions/DataTable"
          }
        }
      }
    },
    "/v2/observations/crosstable": {
      "post": {
        "description": "Returns a tabular view of subject counts, using specified constraints for rows and columns.\nEach cell represents a number of subjects computed as an intersection of column set, row set\nand selected subject set that is specified by subject constraint.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "description": "Data table parameters",
            "schema": {
              "type": "object",
              "properties": {
                "rowConstraints": {
                  "required": true,
                  "type": "array",
                  "items": {
                    "type": "object"
                  },
                  "description": "List of constraint specifications. Each constraint is a row header and specifies a row set\npart of the final set computed for a cell.\nExample: `[{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"},\n{\"type\":\"concept\",\"conceptCode\":\"height\"}]`.\n"
                },
                "columnConstraints": {
                  "required": true,
                  "type": "array",
                  "items": {
                    "type": "object"
                  }
                },
                "subjectConstraint": {
                  "required": true,
                  "type": "object",
                  "description": "The constraint for a subject set. In particular, subjectConstraint can be\nof type `patient_set` in order to explicitly specify the id of a set of patients.\nExample: `{\"type\": \"patient_set\", \"patientSetId\": 12345}`.\n"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "$ref": "#/definitions/CrossTable"
          }
        }
      }
    },
    "/v2/patients": {
      "get": {
        "description": "Gets all patients that have an observation that satisfy the given constaint. Only patients that the calling user has access to are returned.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"study_name\",\"studyId\":\"EHR\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "object",
              "properties": {
                "patients": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Patient"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "required": true,
            "in": "body",
            "schema": {
              "type": "object",
              "properties": {
                "constraint": {
                  "type": "string",
                  "description": "see GET parameters"
                }
              },
              "required": [
                "constraint"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "object",
              "properties": {
                "patients": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Patient"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/patients/{id}": {
      "get": {
        "description": "Gets one patient object.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "description": "id to fetch",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns one patient\n",
            "schema": {
              "$ref": "#/definitions/Patient"
            }
          }
        }
      }
    },
    "/v2/patient_sets": {
      "get": {
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "Gets all patient_sets accessible by the user.\n",
            "schema": {
              "$ref": "#/definitions/PatientSet"
            }
          }
        }
      },
      "post": {
        "description": "creates a patient set with all patients that have an observation that satisfies the constaint given in the body. The set will only have patients the calling user access to. The constraint used to create the set will be stored in a database.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "name",
            "required": true,
            "in": "query",
            "type": "string"
          },
          {
            "name": "constraint",
            "description": "json that specifies the constraint. Example: `{\"type\":\"study_name\",\"studyId\":\"EHR\"}`.",
            "in": "body",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "an object with the created patient_set or error.\n",
            "schema": {
              "$ref": "#/definitions/PatientSet"
            }
          }
        }
      }
    },
    "/v2/patient_sets/{resultInstanceId}": {
      "get": {
        "parameters": [
          {
            "name": "resultInstanceId",
            "in": "path",
            "description": "ID of the patient set, called resultInstance ID because internally it refers to the result of a query\n",
            "required": true,
            "type": "string"
          }
        ],
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "Returns one patient_set.\n",
            "schema": {
              "$ref": "#/definitions/PatientSet"
            }
          }
        }
      }
    },
    "/v2/concepts": {
      "get": {
        "description": "Gets all concepts the user has access to.\nSimilar to `/v2/dimensions/concept/elements` without any constraint.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "Returns the concepts that the user has access to.\n",
            "schema": {
              "type": "object",
              "properties": {
                "concepts": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/Concept"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/concepts/{conceptCode}": {
      "get": {
        "description": "Gets the concepts with the provided concept code if it exists and the user has access to it.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "conceptCode",
            "required": true,
            "in": "path",
            "description": "Concept code of the concept.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns the concept if it exists and the user has access to it.\n",
            "schema": {
              "$ref": "#/definitions/Concept"
            }
          },
          "404": {
            "description": "No concept exists with the concept code, or the user does not have access to the concept.\n"
          }
        }
      }
    },
    "/v2/tree_nodes": {
      "get": {
        "description": "Gets all tree nodes. Number of nodes can be limited by changing the `root` path and max `depth`. `counts` and `tags` are omitted if not requested.\n",
        "produces": [
          "application/json",
          "application/hal+json"
        ],
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "root",
            "in": "query",
            "type": "string",
            "description": "The node the requested tree starts from. Example: `\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\`."
          },
          {
            "name": "depth",
            "in": "query",
            "type": "integer",
            "description": "The max node depth returned"
          },
          {
            "name": "constraints",
            "in": "query",
            "type": "boolean",
            "description": "Flag if the constraints should be included in the result (always false for hal, defaults to true for json)"
          },
          {
            "name": "counts",
            "in": "query",
            "type": "boolean",
            "description": "Patient and observation counts will be in the response if set to true."
          },
          {
            "name": "tags",
            "in": "query",
            "type": "boolean",
            "description": "Metadata tags will be in the response if set to true."
          }
        ],
        "responses": {
          "200": {
            "description": "A forest stucture if there are several root nodes. For example when there are Public Studies, Private Studies and shared concepts.\n",
            "schema": {
              "type": "object",
              "properties": {
                "tree_nodes": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/TreeNode"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/tree_nodes/clear_cache": {
      "get": {
        "description": "Clears tree node and counts caches.\nOnly for administrators.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "The cache has been cleared.\n"
          }
        }
      }
    },
    "/v2/tree_nodes/rebuild_cache": {
      "get": {
        "description": "Clears tree node and counts caches and rebuilds the tree node cache per user.\nOnly for administrators.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "Clearing and rebuilding the cache has been started.\n"
          },
          "503": {
            "description": "A rebuild operation is already in progress.\n"
          }
        }
      }
    },
    "/v2/system/after_data_loading_update": {
      "get": {
        "description": "This endpoint should be called after loading, deleting or updating data in the database. Clears tree nodes, counts caches, patient sets and bitsets. Updates data for subscribed user queries.\nOnly for administrators.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "The cache and patient sets have been cleared. The data for subscribed user queries is updated.\n"
          }
        }
      }
    },
    "/v2/storage": {
      "get": {
        "description": "Gets a list of all storage systems.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array with all storage systems.\n",
            "schema": {
              "type": "object",
              "properties": {
                "storageSystems": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/StorageSystem"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Adds a new storage system with the properties provided in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "systemType": {
                  "type": "string"
                },
                "url": {
                  "type": "string"
                },
                "systemVersion": {
                  "type": "string"
                },
                "singleFileCollections": {
                  "type": "boolean"
                }
              }
            }
          }
        ],
        "responses": {
          "201": {
            "description": "returns the added storage system object.\n",
            "schema": {
              "$ref": "#/definitions/StorageSystem"
            }
          }
        }
      }
    },
    "/v2/storage/{id}": {
      "get": {
        "description": "Gets the storage system with the given `id`\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "returns the storage system object.\n",
            "schema": {
              "$ref": "#/definitions/StorageSystem"
            }
          }
        }
      },
      "put": {
        "description": "Updates the storage system with given id with the values in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          },
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "systemType": {
                  "type": "string"
                },
                "url": {
                  "type": "string"
                },
                "systemVersion": {
                  "type": "string"
                },
                "singleFileCollections": {
                  "type": "boolean"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "returns the updated storage system object.\n",
            "schema": {
              "$ref": "#/definitions/StorageSystem"
            }
          }
        }
      },
      "delete": {
        "description": "Deletes the storage system with the given id.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "204": {
            "description": "returns null"
          }
        }
      }
    },
    "/v2/files": {
      "get": {
        "description": "Gets a list of all file links.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array with all file links.\n",
            "schema": {
              "type": "object",
              "properties": {
                "files": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/FileLink"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Adds a new file link with the properties provided in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "sourceSystem": {
                  "type": "integer"
                },
                "study": {
                  "type": "string"
                },
                "uuid": {
                  "type": "string"
                }
              }
            }
          }
        ],
        "responses": {
          "201": {
            "description": "returns the added file link object.\n",
            "schema": {
              "$ref": "#/definitions/FileLink"
            }
          }
        }
      }
    },
    "/v2/files/{id}": {
      "get": {
        "description": "Gets the file link  with the given `id`.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "returns the file link object.\n",
            "schema": {
              "$ref": "#/definitions/FileLink"
            }
          }
        }
      },
      "put": {
        "description": "Updates the file link  with given id with the values provided in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          },
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "sourceSystem": {
                  "type": "integer"
                },
                "study": {
                  "type": "string"
                },
                "uuid": {
                  "type": "string"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "returns the updated file link object.\n",
            "schema": {
              "$ref": "#/definitions/FileLink"
            }
          }
        }
      },
      "delete": {
        "description": "Deletes the file link with the given id.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "204": {
            "description": "returns null"
          }
        }
      }
    },
    "/v2/studies/{studyId}/files": {
      "get": {
        "description": "Gets a list of all file links related to a study.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "studyId",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array with all file links related to a study.\n",
            "schema": {
              "type": "object",
              "properties": {
                "files": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/FileLink"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/arvados/workflows": {
      "get": {
        "description": "Gets a list of all supported workflows.\n",
        "tags": [
          "v2",
          "arvados"
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array with all supported workflows.\n",
            "schema": {
              "type": "object",
              "properties": {
                "supportedWorkflows": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/SupportedWorkflow"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Adds a new supported Workflow with the properties provided in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2",
          "arvados"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "arvadosInstanceUrl": {
                  "type": "string"
                },
                "uuid": {
                  "type": "string"
                },
                "description": {
                  "type": "string"
                },
                "arvadosVersion": {
                  "type": "string"
                },
                "defaultParams": {
                  "description": "a map of key value pairs",
                  "type": "object"
                }
              }
            }
          }
        ],
        "responses": {
          "201": {
            "description": "returns the created supported workflow object.\n",
            "schema": {
              "$ref": "#/definitions/SupportedWorkflow"
            }
          }
        }
      }
    },
    "/v2/arvados/workflows/{id}": {
      "get": {
        "description": "Gets the supported workflow with the given `id`.\n",
        "tags": [
          "v2",
          "arvados"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "returns the supported workflow object.\n",
            "schema": {
              "$ref": "#/definitions/SupportedWorkflow"
            }
          }
        }
      },
      "put": {
        "description": "Updates the supported workflow with given id with the values in the body. Calling user must have `admin` permissions.\n",
        "tags": [
          "v2",
          "arvados"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          },
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "arvadosInstanceUrl": {
                  "type": "string"
                },
                "uuid": {
                  "type": "string"
                },
                "description": {
                  "type": "string"
                },
                "arvadosVersion": {
                  "type": "string"
                },
                "defaultParams": {
                  "description": "a map of key value pairs",
                  "type": "object"
                }
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "returns the modified supported workflow object.\n",
            "schema": {
              "$ref": "#/definitions/SupportedWorkflow"
            }
          }
        }
      },
      "delete": {
        "description": "Deletes the supported workflow with the given id.\n",
        "tags": [
          "v2",
          "arvados"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "204": {
            "description": "returns null"
          }
        }
      }
    },
    "/v2/dimensions/{dimensionName}/elements": {
      "get": {
        "description": "Gets all elements from a dimension of given name that satisfy the constaint if given.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "dimensionName",
            "in": "path",
            "required": true,
            "type": "string"
          },
          {
            "name": "constraint",
            "required": false,
            "in": "query",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns list of all elements from the given dimension that user has access to.\n",
            "schema": {
              "$ref": "#/definitions/DimensionElements"
            }
          }
        }
      },
      "post": {
        "description": "Works the same as GET, but with support for longer constraints. Use this, if the total length of the URL may be longer than the maximum length.\n",
        "tags": [
          "v2"
        ],
        "consumes": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "dimensionName",
            "in": "path",
            "required": true,
            "type": "string"
          },
          {
            "name": "constraint",
            "required": false,
            "in": "body",
            "description": "json that specifies the constraint. Example: `{\"type\":\"concept\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns list of all elements from the given dimension that user has access to.\n",
            "schema": {
              "$ref": "#/definitions/DimensionElements"
            }
          }
        }
      }
    },
    "/v2/export/job": {
      "post": {
        "description": "Creates a new asynchronous data export job.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "name",
            "required": false,
            "in": "query",
            "description": "(optional) name of the export job (has to be unique for the user). If it is not specified, a default name will be created.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "an object with the created export job or error.\n",
            "schema": {
              "$ref": "#/definitions/ExportJob"
            }
          }
        }
      }
    },
    "/v2/export/{jobId}/run": {
      "post": {
        "description": "Runs the specified data export job asynchronously. Creates a hypercube for each element from {elements} with PatientSetsConstraint for given `id`\nand serialises it to specified `fileFormat`. Output stream is saved on the server as .zip file.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "id of the export job. The job has to have a status `Created`.",
            "type": "string"
          },
          {
            "name": "body",
            "required": true,
            "in": "body",
            "description": "contains json object to initialize export job.",
            "schema": {
              "type": "object",
              "properties": {
                "id": {
                  "type": "array",
                  "description": "result instance ids. To specify more than one value, multiple parameter instances format is required instead of multiple values for a single instance. Example: `id=3425&id=98532&id=...`.",
                  "items": {
                    "type": "integer"
                  }
                },
                "constraint": {
                  "type": "string",
                  "description": "observations that meet this constraint get exported. Note: whether id or constraint has to be supplied, not both. Example: `constraint={\"type\": \"study_name\", \"studyId\": \"EHR\"}`."
                },
                "includeMeasurementDateColumns": {
                  "type": "boolean",
                  "description": "Flag specifies whether to include measurement date columns."
                },
                "elements": {
                  "type": "string",
                  "description": "json that specifies the list of pairs: `[{dataType:${dataType}, format:${fileFormat}, dataView:${dataView}]`,\nwhere `dataType` is a type of the data you want to retrieve, either `clinical` for clinical data,\nor one of the supported high dimensional data types and `format` is one of the supported file formats\nyou want to export current data type to. The tabular flag (optional, false by default) specifies whether\nrepresent hypercube data as wide filer format where patients are rows and columns are variables.\nExample: `[{\"dataType\":clinical, \"format\":TSV, \"tabular\":true},{\"dataType\":rnaseq_transcript, \"format\":TSV}]`.\n\n`dataView` is optional, it can be `\"surveyTable\"` or `\"dataTable\"`, resulting in either a survey\ntable or a data table export. When not set the export will default to a plain hypercube export.\n"
                },
                "tableConfig": {
                  "type": "string",
                  "description": "`tableConfig` is only used for data table exports. It must be a JSON map containing the\nkeys `rowDimensions`, `columnDimensions`, `rowSort` and `columnSort`, with\nthe same meanings and optionality as in the `/v2/observations/table` call.\n"
                }
              },
              "required": [
                "id",
                "elements"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "an object with the run export job with status 'Started' or 'Error'.\n",
            "schema": {
              "$ref": "#/definitions/ExportJob"
            }
          }
        }
      }
    },
    "/v2/export/{jobId}/download": {
      "get": {
        "description": "Gets zipped file with exported data, created by specified job.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "id of the export job. The job has to have a status `Completed`.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Zip file stream with expoted data.\n",
            "schema": {
              "type": "file"
            }
          }
        }
      }
    },
    "/v2/export/{jobId}/status": {
      "get": {
        "description": "Gets a status of specified data export job. Deprecated. Use /v2/export/{jobId} instead.\n",
        "deprecated": true,
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "Id of the export job.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "an object with the export job, including its status.\n",
            "schema": {
              "$ref": "#/definitions/ExportJob"
            }
          }
        }
      }
    },
    "/v2/export/{jobId}": {
      "get": {
        "description": "Gets an export job.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "Id of the export job.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "an object with the export job.\n",
            "schema": {
              "$ref": "#/definitions/ExportJob"
            }
          }
        }
      },
      "delete": {
        "description": "Deletes an export job.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "Id of the export job.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "200 http status when job has been deleted.\n"
          }
        }
      }
    },
    "/v2/export/{jobId}/cancel": {
      "post": {
        "description": "Cancels an export job.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "jobId",
            "required": true,
            "in": "path",
            "description": "Id of the export job.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "200 http status when job has been cancelled.\n"
          }
        }
      }
    },
    "/v2/export/jobs": {
      "get": {
        "description": "Gets all export jobs accessible by the user.\n",
        "tags": [
          "v2"
        ],
        "produces": [
          "application/json"
        ],
        "responses": {
          "200": {
            "description": "Returns a list of export jobs\n",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ExportJob"
              }
            }
          }
        }
      }
    },
    "/v2/export/data_formats": {
      "post": {
        "description": "Analyses the constraint and gets result data formats, including `clinical` type and high dimensional types.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "constraint",
            "required": true,
            "in": "body",
            "description": "json that specifies the constraint. Example: `{\"type\":\"study_name\",\"studyId\":\"EHR\"}`.",
            "type": "object"
          }
        ],
        "responses": {
          "200": {
            "description": "Returns a list of known data formats for specified sets.\nExample: `{ \"dataFormats\": [\"clinical\", \"mrna\", \"rnaseq_transcript\"] }`\n",
            "schema": {
              "type": "object",
              "properties": {
                "dataFormats": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/export/file_formats": {
      "get": {
        "description": "Gets supported export file formats.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "dataView",
            "required": false,
            "in": "query",
            "description": "Data view.",
            "type": "string"
          }
        ],
        "produces": [
          "application/json"
        ],
        "responses": {
          "200": {
            "description": "Returns a list of file formats that data can be exported to. Example: `{ \"fileFormats\": [\"TSV\"] }`\n",
            "schema": {
              "type": "object",
              "properties": {
                "supportedFileFormats": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/queries": {
      "get": {
        "description": "Gets a list of all queries for current user.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array with all queries for current user.\n",
            "schema": {
              "type": "object",
              "properties": {
                "queries": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/UserQuery"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "description": "Adds a new user query with the properties provided in the body.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "patientsQuery": {
                  "type": "object"
                },
                "observationsQuery": {
                  "type": "object"
                },
                "bookmarked": {
                  "type": "boolean"
                },
                "subscribed": {
                  "type": "boolean"
                },
                "subscriptionFreq": {
                  "type": "string"
                },
                "queryBlob": {
                  "type": "object"
                }
              }
            }
          }
        ],
        "responses": {
          "201": {
            "description": "returns the added user query object.\n",
            "schema": {
              "$ref": "#/definitions/UserQuery"
            }
          }
        }
      }
    },
    "/v2/queries/{id}": {
      "get": {
        "description": "Gets the user query with the given `id`\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "returns the user query object.\n",
            "schema": {
              "$ref": "#/definitions/UserQuery"
            }
          }
        }
      },
      "put": {
        "description": "Updates the user query with given id with the values in the body.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          },
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "bookmarked": {
                  "type": "boolean"
                },
                "subscribed": {
                  "type": "boolean"
                },
                "subscriptionFreq": {
                  "type": "string"
                }
              }
            }
          }
        ],
        "responses": {
          "204": {
            "description": "replies with the 204 http status if the update finished successfully."
          }
        }
      },
      "delete": {
        "description": "Deletes the user query with the given id.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "type": "integer"
          }
        ],
        "responses": {
          "204": {
            "description": "returns null"
          }
        }
      }
    },
    "/v2/pedigree/relation_types": {
      "get": {
        "description": "Gets the list of the relation types.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "200": {
            "description": "returns the list of the relation types.\n",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/RelationType"
              }
            }
          }
        }
      }
    },
    "/v2/queries/{queryId}/sets": {
      "get": {
        "description": "Gets a list of query result change entries by query id.\nHistory of data changes for specific query.\n",
        "tags": [
          "v2"
        ],
        "parameters": [
          {
            "name": "queryId",
            "required": true,
            "in": "path",
            "description": "Id of a user query.",
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "an object that contains an array of all querySets related to the query with a set change history.\n",
            "schema": {
              "type": "object",
              "properties": {
                "querySets": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/UserQuerySet"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/v2/queries/sets/scan": {
      "post": {
        "description": "Scans for changes in results of the stored user queries and updates stored sets.\nOnly for administrators.\n",
        "tags": [
          "v2"
        ],
        "responses": {
          "201": {
            "description": "Successful response",
            "schema": {
              "type": "object",
              "properties": {
                "numberOfUpdatedSets": {
                  "type": "integer"
                }
              }
            }
          }
        }
      }
    }
  },
  "definitions": {
    "Version": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string"
        },
        "prefix": {
          "type": "string",
          "description": "the url prefix where this api can be found"
        },
        "version": {
          "type": "string",
          "description": "the full version string"
        },
        "major": {
          "type": "integer"
        },
        "minor": {
          "type": "integer"
        },
        "patch": {
          "type": "integer"
        },
        "tag": {
          "type": "string"
        },
        "features": {
          "type": "object",
          "properties": {
            "features": {
              "type": "integer",
              "description": "string keys and numeric values that indicate features and their revision level. These are only present for -dev versions."
            }
          }
        }
      },
      "required": [
        "id",
        "prefix",
        "major"
      ]
    },
    "LegacyObservation": {
      "type": "object",
      "properties": {
        "subject": {
          "$ref": "#/definitions/Patient"
        },
        "label": {
          "type": "string"
        },
        "value": {
          "type": "string"
        }
      }
    },
    "OntologyTerm": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "key": {
          "type": "string"
        },
        "fullName": {
          "type": "string"
        },
        "type": {
          "type": "string"
        }
      }
    },
    "JsonStudy": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string"
        },
        "ontologyTerm": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "key": {
              "type": "string"
            },
            "fullName": {
              "type": "string"
            },
            "type": {
              "type": "string"
            }
          }
        }
      }
    },
    "HalStudy": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string"
        },
        "_links": {
          "type": "object",
          "properties": {
            "self": {
              "type": "object",
              "properties": {
                "href": {
                  "type": "string"
                }
              }
            }
          }
        },
        "_embedded": {
          "type": "object",
          "properties": {
            "ontologyTerm": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "key": {
                  "type": "string"
                },
                "fullName": {
                  "type": "string"
                },
                "type": {
                  "type": "string",
                  "default": "STUDY"
                },
                "_links": {
                  "type": "object",
                  "properties": {
                    "self": {
                      "type": "object",
                      "properties": {
                        "href": {
                          "type": "string"
                        }
                      }
                    },
                    "observations": {
                      "type": "object",
                      "properties": {
                        "href": {
                          "type": "string"
                        }
                      }
                    },
                    "children": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "href": {
                            "type": "string"
                          },
                          "title": {
                            "type": "string"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "HalStudies": {
      "type": "object",
      "properties": {
        "_links": {
          "type": "object",
          "properties": {
            "self": {
              "type": "object",
              "properties": {
                "href": {
                  "type": "string"
                }
              }
            }
          }
        },
        "_embedded": {
          "type": "object",
          "properties": {
            "studies": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/HalStudy"
              }
            }
          }
        }
      }
    },
    "StorageSystem": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "name": {
          "type": "string"
        },
        "systemType": {
          "type": "string"
        },
        "url": {
          "type": "string"
        },
        "systemVersion": {
          "type": "string"
        },
        "singleFileCollections": {
          "type": "boolean"
        }
      }
    },
    "FileLink": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "name": {
          "type": "string"
        },
        "sourceSystem": {
          "description": "sourceSystem field is an integer ID representing #storage_system from `/v2/storage`",
          "type": "integer"
        },
        "study": {
          "description": "Short case insensitive String identifying tranSMART study, usually study name, given during ETL, can be retrieved by `/v2/studies`.",
          "type": "string"
        },
        "uuid": {
          "type": "string"
        }
      }
    },
    "SupportedWorkflow": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "name": {
          "type": "string"
        },
        "arvadosInstanceUrl": {
          "type": "string"
        },
        "uuid": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "arvadosVersion": {
          "type": "string"
        },
        "defaultParams": {
          "description": "a map of key value pairs",
          "type": "object"
        }
      }
    },
    "TreeNode": {
      "type": "object",
      "properties": {
        "children": {
          "type": "array",
          "description": "A list of treeNodes if there are any children. ",
          "items": {
            "type": "object"
          }
        },
        "fullName": {
          "type": "string",
          "description": "Example: `\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\`"
        },
        "name": {
          "type": "string",
          "description": "Example: SHARED_CONCEPTS_STUDY_A"
        },
        "type": {
          "description": "Example: STUDY",
          "type": "string"
        },
        "visualAttributes": {
          "type": "array",
          "items": {
            "description": "Example: [FOLDER, ACTIVE, STUDY]",
            "type": "string"
          }
        },
        "observationCount": {
          "description": "only available on concept nodes",
          "type": "integer"
        },
        "patientCount": {
          "description": "only available on concept nodes",
          "type": "integer"
        },
        "constraint": {
          "description": "only available on concept nodes; not available for HAL.",
          "type": "object",
          "properties": {
            "type": {
              "description": "Example: `concept`",
              "type": "string"
            },
            "conceptCode": {
              "description": "Example: `age`",
              "type": "string"
            }
          }
        }
      }
    },
    "Study": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "studyID": {
          "type": "string"
        },
        "bioExperimentId": {
          "type": "integer"
        },
        "dimensions": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "metadata": {
          "$ref": "#/definitions/StudyMetadata"
        }
      }
    },
    "StudyMetadata": {
      "type": "object",
      "properties": {
        "conceptToVariableName": {
          "description": "a map from concept code to variable name in the study.",
          "type": "object",
          "properties": {
            "<conceptCode>": {
              "type": "string"
            }
          },
          "additionalProperties": {
            "type": "string"
          }
        }
      }
    },
    "Patient": {
      "type": "object",
      "properties": {
        "age": {
          "type": "integer"
        },
        "birthDate": {
          "type": "string",
          "format": "date"
        },
        "deathDate": {
          "type": "string",
          "format": "date"
        },
        "id": {
          "type": "integer"
        },
        "inTrialId": {
          "type": "integer"
        },
        "maritalStatus": {
          "type": "string"
        },
        "race": {
          "type": "string"
        },
        "religion": {
          "type": "string"
        },
        "sex": {
          "type": "string"
        },
        "trial": {
          "type": "string"
        }
      }
    },
    "PatientSet": {
      "type": "object",
      "properties": {
        "description": {
          "type": "string"
        },
        "errorMessage": {
          "type": "string"
        },
        "id": {
          "type": "integer"
        },
        "setSize": {
          "type": "integer"
        },
        "status": {
          "type": "string"
        },
        "username": {
          "type": "string"
        },
        "requestConstraints": {
          "type": "string"
        },
        "apiVersion": {
          "type": "string"
        }
      }
    },
    "Concept": {
      "type": "object",
      "properties": {
        "conceptCode": {
          "type": "string"
        },
        "conceptPath": {
          "type": "string"
        },
        "name": {
          "type": "string"
        },
        "metadata": {
          "$ref": "#/definitions/VariableMetadata"
        }
      }
    },
    "VariableMetadata": {
      "type": "object",
      "properties": {
        "type": {
          "description": "NUMERIC, DATE, STRING",
          "type": "string"
        },
        "measure": {
          "description": "NOMINAL, ORDINAL, SCALE",
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "width": {
          "type": "integer"
        },
        "decimals": {
          "type": "integer"
        },
        "columns": {
          "type": "integer"
        },
        "valueLabels": {
          "description": "a map from value (of type integer) to label (of type string)",
          "type": "object",
          "properties": {
            "<value>": {
              "type": "string"
            }
          },
          "additionalProperties": {
            "type": "string"
          }
        },
        "missingValues": {
          "type": "array",
          "items": {
            "type": "integer"
          }
        }
      }
    },
    "Counts": {
      "type": "object",
      "properties": {
        "observationCount": {
          "type": "integer"
        },
        "patientCount": {
          "type": "integer"
        }
      }
    },
    "CountsMap": {
      "description": "a map from string to Counts.",
      "properties": {
        "<key>": {
          "$ref": "#/definitions/Counts"
        }
      },
      "additionalProperties": {
        "$ref": "#/definitions/Counts"
      }
    },
    "CountsMapMap": {
      "description": "a map from string to a map from string to Counts.",
      "properties": {
        "<key>": {
          "$ref": "#/definitions/CountsMap"
        }
      },
      "additionalProperties": {
        "$ref": "#/definitions/CountsMap"
      }
    },
    "DataTable": {
      "description": "A JSON object that represents the dataset in a tabular form. The actual row data can be found in\n`rows*.cells`, with the headers and other metadata available in other keys.\n",
      "schema": {
        "type": "object",
        "properties": {
          "columnHeaders": {
            "description": "This property describes the top header of the table. The header can contain multiple rows, each row is\nfound in the `keys` or `elements` property of an item. If the `keys` property is set, it contains\nreferences to dimension elements that are further elaborated in the `column_dimensions` property.\n",
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "dimension": {
                  "type": "string",
                  "description": "the name of a column dimension"
                },
                "keys": {
                  "type": "array",
                  "items": {
                    "oneOf": [
                      {
                        "type": "string"
                      },
                      {
                        "type": "number"
                      }
                    ]
                  },
                  "description": "A list of column headers for one dimension. This list has the same size as the number of columns in\nthe table (unless no column dimensions were specified, in which case this list is empty). The items\ndescribe a dimension element, they refer to keys in the\n`rowDimensions*.elements` dictionary for the given dimension. Further properties of this dimension\nelement can be found there.\n\nThis property is mutually exclusive with the `elements` property.\n"
                },
                "elements": {
                  "type": "array",
                  "items": {
                    "oneOf": [
                      {
                        "type": "string"
                      },
                      {
                        "type": "number"
                      }
                    ]
                  },
                  "description": "A list of column headers for one dimension, but in this property the items do not refer to a\nfurther description, the dimension element is just a single string or number. The string may also\nbe a formatted date.\n\nThe size of this list is equal to the number of columns in the table, or empty if no column\ndimensions were specified.\n\nThis property is mutually exclusive with the `keys` property.\n"
                }
              },
              "required": [
                "dimension"
              ]
            }
          },
          "rows": {
            "description": "This property contains the row data and the left table header. The row data is found in the `row`\nproperty, the left header cells in the `dimensions` property.\n",
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "rowHeaders": {
                  "description": "The left header cells for this row. Each cell contains the element or a reference to the element of\none row dimension. There is one item for each row dimension in this list, in order.\n",
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dimension": {
                        "type": "string",
                        "description": "The name of this row dimension"
                      },
                      "key": {
                        "type": {
                          "oneOf": [
                            {
                              "type": "string"
                            },
                            {
                              "type": "number"
                            }
                          ]
                        },
                        "description": "Analogous to `columnHeaders*.keys`. The value is a reference to a dimension element\nspecified in `rowDimensions*.elements` for the specified dimension.\n\nThis property is mutually exclusive with the `element` property.\n"
                      },
                      "element": {
                        "type": {
                          "oneOf": [
                            {
                              "type": "string"
                            },
                            {
                              "type": "number"
                            }
                          ]
                        },
                        "description": "Analogous to `columnHeaders*.elements`. This is a single dimension element if that dimension\nelement is a single string, number or date. A date is represented as a formatted string.\n\nThis property is mutually exclusive with the `key` property.\n"
                      }
                    },
                    "required": [
                      "dimension"
                    ]
                  }
                },
                "cells": {
                  "type": "array",
                  "items": {
                    "oneOf": [
                      {
                        "type": "string"
                      },
                      {
                        "type": "number"
                      },
                      {
                        "type": "array",
                        "items": {
                          "oneOf": [
                            {
                              "type": "string"
                            },
                            {
                              "type": "number"
                            }
                          ]
                        }
                      }
                    ]
                  },
                  "description": "The row data for a single table row. The items are strings or numbers or dates (represented as a\nformatted string). The length of each row is the same, and is the same as the size of the\n`columnHeaders*.keys` and `columnHeaders*.elements` lists.\n\nThe item can also be an array of strings, numbers or dates. This happens if multiple values fall\ninto the same table cell.\n"
                }
              }
            }
          },
          "rowDimensions": {
            "description": "This property contains a description of the row dimensions. It is a list of objects, with each object\ndescribing one dimension. If the dimension has its elements inlined the description here will only\ncontain the dimension name. If the headers contain keys the full dimension elements can be found in the\n`elements` map. This map is indexed by the key found in the headers. The elements themselves are a\nfree-form maps with the properties depending on the dimension type, but every element has at least a\n`label` property (which is often the same as the key, though not always).\n",
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "the dimension name"
                },
                "elements": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "object",
                    "properties": {
                      "label": {
                        "type": "string"
                      }
                    },
                    "additionalProperties": true,
                    "description": "A dimension element, the available properties depend on the dimension, but 'label' is always\nincluded.\n"
                  },
                  "description": "The dimension elements, indexed by the key used in the header. This property is only present if the\ndimension elements are compound and referenced by keys, simple elements will be inlined in the header.\n"
                }
              },
              "required": [
                "name"
              ]
            }
          },
          "columnDimensions": {
            "description": "The same as `rowDimensions`, but for the column dimensions.\n",
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "the dimension name"
                },
                "elements": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "object",
                    "properties": {
                      "label": {
                        "type": "string"
                      }
                    },
                    "additionalProperties": true
                  }
                }
              },
              "required": [
                "name"
              ]
            }
          },
          "sort": {
            "description": "This property lists the sort order that was used in creating the dataset. This will generally include all\nthe dimensions that were specified as row or column dimensions. This property includes both the row\nand column dimensions, with row dimensions first and column dimensions followed by that.\n",
            "type": "array",
            "items": {
              "$ref": "#/definitions/SortResponse"
            }
          },
          "offset": {
            "type": "integer",
            "description": "the offset of the first returned row in the full dataset"
          },
          "rowCount": {
            "type": "integer",
            "description": "The total number of rows available. This property is only set if the total is known, which is\ngenerally only the case if the last row is part of this result.\n"
          }
        },
        "required": [
          "columnHeaders",
          "rows",
          "rowDimensions",
          "sort",
          "offset"
        ]
      }
    },
    "CrossTable": {
      "description": "A JSON object that represents the subject counts in a tabular form. List of rows, each of which contains\na list with numbers of subjects for each cell.\n",
      "schema": {
        "type": "object",
        "properties": {
          "rows": {
            "description": "This property contains the row data. Items are cells, each cell is a number of subject, computed as a result\nof an intersection of constraints specified for a row, for a column and for a selected subject set.\n",
            "type": "array",
            "items": {
              "type": "number"
            }
          }
        },
        "required": [
          "rows"
        ]
      }
    },
    "Observation": {
      "type": "object",
      "properties": {
        "conceptCode": {
          "type": "string"
        },
        "encounterNum": {
          "type": "integer"
        },
        "endDate": {
          "type": "string",
          "format": "datetime"
        },
        "instanceNum": {
          "type": "integer"
        },
        "locationCd": {
          "type": "string"
        },
        "modifierCd": {
          "type": "string"
        },
        "numberValue": {
          "type": "integer"
        },
        "patient": {
          "type": "object",
          "properties": {
            "id": {
              "type": "integer"
            }
          }
        },
        "providerId": {
          "type": "string"
        },
        "sourcesystemCd": {
          "type": "string"
        },
        "startDate": {
          "type": "string",
          "format": "datetime"
        },
        "textValue": {
          "type": "string"
        },
        "trialVisit": {
          "type": "object",
          "properties": {
            "id": {
              "type": "integer"
            }
          }
        },
        "valueFlag": {
          "type": "string"
        },
        "valueType": {
          "type": "string"
        }
      }
    },
    "Observations": {
      "type": "object",
      "properties": {
        "header": {
          "type": "object",
          "properties": {
            "dimensionDeclarations": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/DimensionDeclaration"
              }
            },
            "sort": {
              "type": "object",
              "properties": {
                "dimensionIndex": {
                  "type": "number",
                  "description": "An index into the dimensionDeclarations list, indicating the dimension on which the result is sorted.\n"
                },
                "sortOrder": {
                  "type": "string",
                  "enum": [
                    "asc",
                    "desc"
                  ],
                  "description": "'asc' or 'desc', indicating the sort order."
                },
                "field": {
                  "type": "number",
                  "description": "always 0. A non-zero value indicates that a different sorting is used that this version of the api does not know about. This only applies to the protobuf representation, in JSON this property is omitted.\n"
                }
              }
            }
          }
        },
        "cells": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Cell"
          }
        },
        "footer": {
          "type": "object",
          "properties": {
            "dimensions": {
              "type": "array",
              "items": {
                "type": "array",
                "items": {
                  "$ref": "#/definitions/DimensionValue"
                }
              }
            }
          }
        }
      }
    },
    "DimensionDeclaration": {
      "type": "object",
      "properties": {
        "inline": {
          "description": "If true, this dimension will be inlined in the cell. Only present if true.",
          "type": "boolean"
        },
        "fields": {
          "description": "Fields is omitted if the dimension consists of one field.",
          "type": "array",
          "items": {
            "$ref": "#/definitions/Field"
          }
        },
        "name": {
          "type": "string"
        },
        "type": {
          "description": "STRING, INTEGER, DATE, OBJECT",
          "type": "string"
        }
      }
    },
    "Field": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "type": {
          "description": "STRING, INTEGER, DATE",
          "type": "string"
        }
      }
    },
    "Cell": {
      "type": "object",
      "description": "numericValue or stringValue, never both.",
      "properties": {
        "dimensionIndexes": {
          "description": "The index in the array is equal to the index of the dimension in the dimensions array in the footer. The number is the index of the dimensionValue in the dimension.",
          "type": "array",
          "items": {
            "type": "integer"
          }
        },
        "inlineDimensions": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/DimensionValue"
          }
        },
        "numericValue": {
          "type": "number"
        },
        "stringValue": {
          "type": "string"
        }
      }
    },
    "DimensionValue": {
      "type": "object",
      "description": "The structure of this value is described in the header. The order of the dimensionValues is determined by the order of the dimensionDeclaration in the header."
    },
    "DimensionElements": {
      "type": "object",
      "properties": {
        "apiVersion": {
          "type": "string"
        },
        "elements": {
          "description": "List of dimension elements with properties specific to a given dimension.",
          "type": "array",
          "items": {
            "type": "object"
          }
        }
      }
    },
    "ExportJob": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "jobName": {
          "type": "string"
        },
        "jobStatus": {
          "type": "string"
        },
        "jobStatusTime": {
          "type": "string",
          "format": "datetime"
        },
        "userId": {
          "type": "string"
        },
        "viewerURL": {
          "type": "string"
        }
      }
    },
    "UserQuery": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "name": {
          "type": "string"
        },
        "patientsQuery": {
          "type": "object"
        },
        "observationsQuery": {
          "type": "object"
        },
        "apiVersion": {
          "type": "string"
        },
        "bookmarked": {
          "type": "boolean"
        },
        "subscribed": {
          "type": "boolean"
        },
        "subscriptionFreq": {
          "type": "string"
        },
        "createDate": {
          "type": "string",
          "format": "date-time"
        },
        "updateDate": {
          "type": "string",
          "format": "date-time"
        },
        "queryBlob": {
          "type": "object"
        }
      }
    },
    "UserQuerySet": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "queryId": {
          "type": "integer"
        },
        "queryName": {
          "type": "string"
        },
        "setSize": {
          "type": "integer"
        },
        "setTyp": {
          "type": "string"
        },
        "createDate": {
          "type": "string",
          "format": "date-time"
        },
        "objectsAdded": {
          "type": "array",
          "items": {
            "type": "integer"
          }
        },
        "objectsRemoved": {
          "type": "array",
          "items": {
            "type": "integer"
          }
        }
      }
    },
    "AggregatesMap": {
      "type": "object",
      "description": "Map from string to aggregates.",
      "properties": {
        "<conceptCode>": {
          "$ref": "#/definitions/Aggregates"
        }
      },
      "additionalProperties": {
        "$ref": "#/definitions/Aggregates"
      }
    },
    "Aggregates": {
      "description": "Object for numerical aggregates or categorical value counts. Only the value of the requested aggregate type will be present.",
      "type": "object",
      "properties": {
        "numericalValueAggregatesPerConcept": {
          "type": "object",
          "properties": {
            "min": {
              "type": "number"
            },
            "max": {
              "type": "number"
            },
            "avg": {
              "type": "number"
            },
            "count": {
              "type": "number"
            },
            "stdDev": {
              "type": "number"
            }
          }
        },
        "categoricalValueAggregatesPerConcept": {
          "type": "object",
          "properties": {
            "valueCounts": {
              "description": "map from value to count",
              "type": "object",
              "properties": {
                "<value>": {
                  "type": "number"
                }
              },
              "additionalProperties": {
                "type": "number"
              }
            },
            "nullValueCounts": {
              "description": "count of null values",
              "type": "number"
            }
          }
        }
      }
    },
    "RelationType": {
      "description": "Relation type",
      "type": "object",
      "properties": {
        "id": {
          "type": "integer"
        },
        "label": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "symmetrical": {
          "type": "boolean"
        },
        "biological": {
          "type": "boolean"
        }
      }
    },
    "SortSpecification": {
      "description": "Specifies dimension to sort on and the sorting direction.",
      "type": "object",
      "properties": {
        "dimension": {
          "type": "string",
          "description": "the name of the dimension to sort on"
        },
        "sortOrder": {
          "type": "string",
          "description": "'asc' or 'desc', describing if sorting is ascending or descending.\n"
        }
      },
      "required": [
        "dimension",
        "sortOrder"
      ]
    },
    "SortResponse": {
      "description": "Specifies dimension to sort on and the sorting direction.",
      "type": "object",
      "properties": {
        "dimension": {
          "type": "string",
          "description": "the name of the dimension to sort on"
        },
        "sortOrder": {
          "type": "string",
          "description": "'asc' or 'desc', describing if sorting is ascending or descending.\n"
        },
        "userRequested": {
          "type": "boolean",
          "description": "Whether sorting on this dimension was explicitly requested by the client, i.e., set to\ntrue if this dimension was part of the `rowSort` or `columnSort` request parameter.\nThis parameter is optional, the default is `false`.\n"
        }
      },
      "required": [
        "dimension",
        "sortOrder"
      ]
    }
  }
}
;
