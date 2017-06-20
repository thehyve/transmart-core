var spec = {
  "swagger": "2.0",
  "info": {
    "version": "17.1.0",
    "title": "Transmart",
    "license": {
      "name": "Apache 2.0",
      "url": "http://www.apache.org/licenses/LICENSE-2.0.html"
    },
    "description": "\n# OAuth2\nAll calls need an Authorization header. https://wiki.transmartfoundation.org/display/transmartwiki/RESTful+API\n```\nAuthorization:Bearer {token}\n```\n\n# Constraints\nConstraints are used to build queries and are required in the `v2` API. They consist of a `Type` and that type's specific arguments. The implementation is in [Constraint.groovy](../transmart-core-db/src/main/groovy/org/transmartproject/db/multidimquery/query/Constraint.groovy).\n\n## Combinations (And/Or)\nMost often a combination of constraints is needed to get the right result. This can be done by the constraints with type \"and\" and \"or\".\nThey take a list `args` with constraints. All args will be evaluated together on each observation. So an 'and' operator with a `patient_set` and a `concept` will return all observations for the given concept linked to the patient set.\nHowever an `and` constraint with two ConceptConstraints will evaluate to an empty result, as no observation can have two concepts. This is also true even if nested with a different combination because constraints do not know scope.\n(There is also a constraint with type \"combination\" on which the And and Or constraints are built. It does not provide any functionality not provided by And and Or constraints, and it should be considered deprecated for direct usage.)\n\nExample:\n```json\n{\"type\": \"and\",\n \"args\": [\n    {\"type\": \"patient_set\", \"patientIds\": -62},\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}\n    ]\n}\n```\n\n```json\n{\"type\": \"or\",\n \"args\": [\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Blood Pressure\\\\\"}\n    {\"type\": \"concept\", \"path\":\" \\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}\n    ]\n}\n```\n\n## StudyName\nEvaluate if an observation is part of a particular study\n\nExample:\n```json\n{\n  \"type\": \"study_name\",\n  \"studyId\": \"EHR\"\n}\n```\n\n## Concept\nEvaluate if an observation is of a particular Concept. Either by `path` or `conceptCode`.\n\n```json\n{\n  \"type\": \"concept\",\n  \"path\": \"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\",\n  \"conceptCode\": \"HR\"\n}\n```\n\n## Value\nEvaluate if the value of an observation is within the given parameters. It needs a `valueType`, `operator` and `value`.\n  `valueType`: [\\\"NUMERIC\\\", \\\"STRING\\\"]\n  `operator`: [&lt;, &gt;, =, !=, &lt;=, &gt;=, in, like, contains]\n\nExample:\n```json\n{\n  \"type\": \"value\",\n  \"valueType\": \"NUMERIC\",\n  \"operator\": \"=\", \"value\": 176\n}\n```\n\n## Field\nEvaluate if a specific field of an observation is within the given parameters. it needs a `field`, `operator` and `value`.\n  `operator`: [&lt;, &gt;, =, !=, &lt;=, &gt;=, in, like, contains]\n\nExample:\n```json\n{\n  \"type\": \"field\",\n  \"field\": {\n      \"dimension\": \"patient\",\n      \"fieldName\": \"age\",\n      \"type\": \"NUMERIC\"\n      },\n  \"operator\": \"!=\",\n  \"value\": 100\n}\n```\n\n## Time\nEvaluate if an observation is within the specified time period. It needs a `field` the type of which needs to be `DATE`. It needs a time relevant `operator` and a list of `values`.\nThe list must hold one date for the before(<-) and after(->) operator. It must hold two dates for the between(<-->) operator. If the given date field for an observation is empty, the observation will be ignored.\n`operator`: [\"&lt;-\", \"-&gt;\", \"&lt;--&gt;\"]\n\nExample:\n```json\n{\n  \"type\": \"time\",\n  \"field\": {\n      \"dimension\": \"start time\",\n      \"fieldName\": \"startDate\",\n      \"type\": \"DATE\"\n      },\n  \"operator\": \"->\",\n  \"values\": [\"2016-01-01T00:00:00Z\"]\n}\n```\n\n## PatientSet\nEvaluate if an observation is liked to a patient in the set. It needs either a `patientSetId` or a list of `patientIds`.\n\nExample:\n```json\n{\n    \"type\": \"patient_set\",\n    \"patientSetId\": 28820,\n    \"patientIds\": [-62, -63]\n}\n```\n\n## SubSelection\nCreate a subselection of patients, visits, or another dimension element, and then select all observations for these dimension elements.\n\nExample: Select all observations for patients who have a certain diagnosis.\n```json\n{\n    \"type\": \"subselection\",\n    \"dimension\": \"patient\",\n    \"constraint\": {\n        \"type\": \"and\",\n        \"args\": [{\n                \"type\": \"concept\",\n                \"path\": \"\\\\Public Studies\\\\EHR\\\\Diagnosis\\\\\",\n                \"conceptCode\": \"DIAG\"\n            }, {\n                \"type\": \"value\",\n                \"valueType\": \"STRING\",\n                \"operator\": \"=\",\n                \"value\": \"My eye hurts\"\n            }]\n    }\n}\n```\n\n## Temporal\nEvaluate if an observation happened before or after an event. It needs an `operator` and an `event`. Any constraint can be used as an event. Most likely a combination.\n`operator`: [\"&lt;-\", \"-&gt;\", \"exists\"]\n\nExample:\n```json\n{\n    \"type\": \"temporal\",\n    \"operator\": \"exists\",\n    \"event\": {\n          \"type\": \"value\",\n          \"valueType\": \"NUMERIC\",\n          \"operator\": \"=\",\n          \"value\": 60\n          }\n}\n```\n\n## Null\nEvaluate if an specific field of an observation is null. It needs a field.\n\nExample:\n```json\n{\n    \"type\": \"null\",\n    \"field\":{\n        \"dimension\": \"end time\",\n        \"fieldName\": \"endDate\",\n        \"type\": \"DATE\"\n        }\n}\n```\n\n## Modifier\nEvaluate if an observation is linked to the specified modifier. Optionally if that modifier has the specific value. It must have a `path`, `dimensionName` or `modifierCode` and may have `values` in the form of a ValueConstraint.\n\nExample:\n```json\n{\n    \"type\": \"modifier\",\n    \"modifierCode\": \"TNS:SMPL\",\n    \"path\": \"\\\\Public Studies\\\\TUMOR_NORMAL_SAMPLES\\\\Sample Type\\\\\",\n    \"dimensionName\": \"sample_type\",\n    \"values\": {\n        \"type\": \"value\",\n        \"valueType\": \"STRING\",\n        \"operator\": \"=\",\n        \"value\": \"Tumor\"\n        }\n}\n```\n\n## Negation\nEvaluate if for an observation the given `arg` is false. `arg` is a constraint.\n\nExample:\n```json\n{\n    \"type\": \"negation\",\n    \"arg\": {\n        \"type\": \"patient_set\",\n        \"patientIds\": [-62,-52,-42]\n        }\n}\n```\nreturns all observations not linked to patient with id -62, -52 or -42\n\n## Biomarker\nUsed to filter high dimensional observations. It needs a 'biomarkerType' and a 'params' object. It can only be used\nwhen retrieving high dimensional data, and if so needs to be specified in a separate url parameter\n`biomarker_constraint`.\n`biomarkerType`: `[\"transcripts\", \"genes\"]`.\n\nExample:\n```json\n{\n    \"type\": \"biomarker\",\n    \"biomarkerType\": \"genes\",\n    \"params\": {\n        \"names\": [\"TP53\"]\n        }\n}\n```\n\n## True\n**!!WARNING!!** Use mainly for testing.  \nThe most basic of constraints. Evaluates to true for all observations. This returns all observations the requesting user has access to.\n\nExample:\n```json\n{\n    \"type\": \"true\"\n}\n```\n\n\n# Response types\n#### application/json\nAll calls support json. however this might not always be the best option. You will find schemas for the responses in this documentation.\n\n#### `application/hal+json`\nOnly the tree_node endpoint supports the application/hal+json format.\n\n#### `application/x-protobuf`\nCalls that return observations support protobuf as a more efficient binary format. The description of the protobuf object can be found in [observations.proto](../transmart-rest-api/src/protobuf/v2/observations.proto). Information on [google protobuf](https://developers.google.com/protocol-buffers/).\n"
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
      "authorizationUrl": "/oauth/authorize?response_type=token&client_id={client_id}&redirect_uri={redirect}",
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
                      "$ref": "#/definitions/version"
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
              "$ref": "#/definitions/version"
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
              "$ref": "#/definitions/hal+jsonStudies"
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
              "$ref": "#/definitions/jsonStudy"
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
                    "$ref": "#/definitions/ontologyTerm"
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
              "$ref": "#/definitions/ontologyTerm"
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
                    "$ref": "#/definitions/patient"
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
              "$ref": "#/definitions/patient"
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
                    "$ref": "#/definitions/patient"
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
                "$ref": "#/definitions/v1observation"
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
                "$ref": "#/definitions/v1observation"
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
                "$ref": "#/definitions/v1observation"
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
                    "$ref": "#/definitions/v2Study"
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
              "$ref": "#/definitions/v2Study"
            }
          },
          "404": {
            "description": "Study not found\n"
          }
        }
      }
    },
    "/v2/studyId/{studyId}": {
      "get": {
        "description": "Gets the study with study id `id`.\n",
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
              "$ref": "#/definitions/v2Study"
            }
          },
          "404": {
            "description": "Study not found\n"
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
              "$ref": "#/definitions/observations"
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
            "name": "type",
            "required": true,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          },
          {
            "name": "constraint",
            "required": true,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          },
          {
            "name": "biomarker_constraint",
            "required": false,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          },
          {
            "name": "projection",
            "required": false,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Dimensions are described in the `header`. The order in which they appear in the header, determines the order in which they appear in the `cells` and footer. The value in the `dimensionIndexes` corresponds to the values in the `footer`.\n",
            "schema": {
              "$ref": "#/definitions/observations"
            }
          }
        }
      }
    },
    "/v2/observations/aggregate": {
      "get": {
        "description": "Calculates and returns an aggregate value. Supported aggregate types are 'min', 'max', 'average', 'count', and\n'values'. The first three require numeric variables, the last one categorical variables.\n",
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
          },
          {
            "name": "type",
            "required": true,
            "in": "query",
            "description": "'min', 'max', 'average', 'count', or 'values'. This parameter can be specified multiple times to retrieve\nmultiple aggregates at once. The 'values' aggregate cannot be combined with the numeric aggregate types.\n",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "return the result in a json object. Example: `{min: 56}`.",
            "schema": {
              "type": "object",
              "description": "only the value of the requested aggregate type will be present.",
              "properties": {
                "min": {
                  "type": "number"
                },
                "max": {
                  "type": "number"
                },
                "average": {
                  "type": "number"
                },
                "values": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  },
                  "description": "A list of the distinct values for categorical variables"
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
            "name": "constraint",
            "required": true,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          },
          {
            "name": "type",
            "required": true,
            "in": "body",
            "description": "see GET parameters. Can be either a string or an array of strings.",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "return the result in a json object. Example: `{min: 56}`.",
            "schema": {
              "type": "object",
              "description": "only the value of the requested aggregate type will be present.",
              "properties": {
                "min": {
                  "type": "number"
                },
                "max": {
                  "type": "number"
                },
                "average": {
                  "type": "number"
                }
              }
            }
          }
        }
      }
    },
    "/v2/observations/count": {
      "get": {
        "description": "Counts the number of observations that satisfy the given constraint.\n",
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
            "description": "Return the result as a json object. Example: `{count: 56}`.",
            "schema": {
              "type": "object",
              "properties": {
                "count": {
                  "type": "integer"
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
            "name": "constraint",
            "required": true,
            "in": "body",
            "description": "see GET parameters",
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Return the result as a json object. Example: `{count: 56}`.",
            "schema": {
              "type": "object",
              "properties": {
                "count": {
                  "type": "integer"
                }
              }
            }
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
                    "$ref": "#/definitions/patient"
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
            "name": "constraint",
            "required": true,
            "in": "body",
            "description": "see GET parameters",
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
                    "$ref": "#/definitions/patient"
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
              "$ref": "#/definitions/patient"
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
              "$ref": "#/definitions/patient_set"
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
              "$ref": "#/definitions/patient_set"
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
              "$ref": "#/definitions/patient_set"
            }
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
                    "$ref": "#/definitions/treeNode"
                  }
                }
              }
            }
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
                    "$ref": "#/definitions/storageSystem"
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
              "$ref": "#/definitions/storageSystem"
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
              "$ref": "#/definitions/storageSystem"
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
              "$ref": "#/definitions/storageSystem"
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
                    "$ref": "#/definitions/fileLink"
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
              "$ref": "#/definitions/fileLink"
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
              "$ref": "#/definitions/fileLink"
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
              "$ref": "#/definitions/fileLink"
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
                    "$ref": "#/definitions/fileLink"
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
                    "$ref": "#/definitions/supportedWorkflow"
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
              "$ref": "#/definitions/supportedWorkflow"
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
              "$ref": "#/definitions/supportedWorkflow"
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
              "$ref": "#/definitions/supportedWorkflow"
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
    }
  },
  "definitions": {
    "version": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "required": true
        },
        "prefix": {
          "type": "string",
          "required": true,
          "description": "the url prefix where this api can be found"
        },
        "version": {
          "type": "string",
          "description": "the full version string"
        },
        "major": {
          "type": "integer",
          "required": true
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
      }
    },
    "v1observation": {
      "type": "object",
      "properties": {
        "subject": {
          "$ref": "#/definitions/patient"
        },
        "label": {
          "type": "string"
        },
        "value": {
          "type": "string"
        }
      }
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
    },
    "jsonStudy": {
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
    "hal+jsonStudy": {
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
    "hal+jsonStudies": {
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
                "$ref": "#/definitions/hal+jsonStudy"
              }
            }
          }
        }
      }
    },
    "storageSystem": {
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
    "fileLink": {
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
    "supportedWorkflow": {
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
    "treeNode": {
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
          "description": "only available on ConceptNodes",
          "type": "integer"
        },
        "patientCount": {
          "description": "only available on ConceptNodes",
          "type": "integer"
        },
        "constraint": {
          "description": "only available on ConceptNodes",
          "type": "object",
          "properties": {
            "type": {
              "description": "Example: ConceptConstraint",
              "type": "string"
            },
            "path": {
              "description": "Example: `\\Public Studies\\CLINICAL_TRIAL\\Demography\\Age\\`",
              "type": "string"
            }
          }
        }
      }
    },
    "v2Study": {
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
        }
      }
    },
    "patient": {
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
    "patient_set": {
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
    "observation": {
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
    "observations": {
      "type": "object",
      "properties": {
        "header": {
          "type": "object",
          "properties": {
            "dimensionDeclarations": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/dimensionDeclaration"
              }
            }
          }
        },
        "cells": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/cell"
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
                  "$ref": "#/definitions/dimensionValue"
                }
              }
            }
          }
        }
      }
    },
    "dimensionDeclaration": {
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
            "$ref": "#/definitions/field"
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
    "field": {
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
    "cell": {
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
            "$ref": "#/definitions/dimensionValue"
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
    "dimensionValue": {
      "type": "object",
      "description": "The structure of this value is described in the header. The order of the dimensionValues is determined by the order of the dimensionDeclaration in the header."
    }
  }
}
;
