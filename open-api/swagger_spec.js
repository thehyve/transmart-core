var spec = {
    "swagger": "2.0",
    "info": {
        "version": "17.1.0",
        "title": "Transmart",
        "description": "### oauth2\nAll calls need Authorization. A Authorization:Bearer {token} headed should be provided when doing a request.\n\n### Constraints\nThe v2 API makes use of [constraints](https://github.com/thehyve/transmart-upgrade/blob/swagger-doc/open-api/constraints.md). Read the documentation on github or the [code](https://github.com/thehyve/transmart-upgrade/blob/master/transmart-core-db/src/main/groovy/org/transmartproject/db/multidimquery/query/Constraint.groovy) to found out how to use them.\n\n### application/json\nAll calls support json. however this might not always be the best option. You will find schema's for the responses in this documentation.\n\n### application/hal+json\nOnly the tree_node endpoint supports the application/hal+json format.\n\n### application/x-protobuf\nCalls that return observations support brotobuf as a more efficient binary format. The description of the protobuf object can be found in the [observations.proto](https://github.com/thehyve/transmart-upgrade/blob/master/transmart-rest-api/src/protobuf/v2/observations.proto). Information on [google protobuf](https://developers.google.com/protocol-buffers/).\n"
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
                            "$ref": "#/definitions/hal+jsonStudys"
                        }
                    }
                }
            }
        },
        "/v1/studies/{studyid}": {
            "get": {
                "description": "Gets all `Study` objects.\n",
                "tags": [
                    "v1"
                ],
                "parameters": [
                    {
                        "name": "studyid",
                        "in": "path",
                        "description": "username to fetch",
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
                "description": "Gets all `Study` objects.\n",
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
                "description": "Gets all `Study` objects.\n",
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
                        "description": "Study ID of the study for which concepts will be fetched",
                        "required": true,
                        "type": "string"
                    }
                ],
                "description": "Gets all `Study` objects.\n",
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
                        "description": "body should be query definition in a subset of i2b2s XML schema.",
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
                    "200": {
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
        "/v2/observations": {
            "get": {
                "description": "Gets all `observations` that evaluate to true for the given constaint. Only observations the calling user has acces to are returned.\n",
                "tags": [
                    "v2"
                ],
                "produces": [
                    "application/json",
                    "application/x-protobuf"
                ],
                "parameters": [
                    {
                        "name": "constraint",
                        "required": true,
                        "in": "query",
                        "description": "json that describes the request, Example: {\"type\":\"StudyNameConstraint\",\"studyId\":\"EHR\"}",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "Dimentions are described in the `header`. The order in which they appear in the header, determens the order in which they appear in the `cells` and footer. The value in the `dimensionIndexes` corresponds to the values in the `footer`\n",
                        "schema": {
                            "$ref": "#/definitions/observations"
                        }
                    }
                }
            }
        },
        "/v2/observations/aggregate": {
            "get": {
                "description": "calculates and returns the requested aggregate value\n",
                "tags": [
                    "v2"
                ],
                "parameters": [
                    {
                        "name": "constraint",
                        "required": true,
                        "in": "query",
                        "description": "json that describes the request, Example: {\"type\":\"ConceptConstraint\",\"path\":\"\\\\Public Studies\\\\EHR\\\\Vital Signs\\\\Heart Rate\\\\\"}",
                        "type": "string"
                    },
                    {
                        "name": "type",
                        "required": true,
                        "in": "query",
                        "description": "min, max, average",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "return the result in a json object Example: {min:56}",
                        "schema": {
                            "type": "object",
                            "description": "only the value from the type in the request will be present",
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
        "/v2/patients": {
            "get": {
                "description": "Gets all `patients` that have an observation that evaluate to true for the given constaint. Only patients the calling user has acces to are returned.\n",
                "tags": [
                    "v2"
                ],
                "parameters": [
                    {
                        "name": "constraint",
                        "required": true,
                        "in": "query",
                        "description": "json that describes the request, Example: {\"type\":\"StudyNameConstraint\",\"studyId\":\"EHR\"}",
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
        "/v2/patient_sets": {
            "post": {
                "description": "creates a `patientSet` with all patients that have an observation that evaluate to true for constaint given in the body. The set will only have patients the calling user acces to.\n",
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
                        "description": "json that describes the set, Example: {\"type\":\"StudyNameConstraint\",\"studyId\":\"EHR\"}",
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
                                }
                            }
                        }
                    }
                }
            }
        },
        "/v2/high_dim": {
            "get": {
                "description": "Gets all high dimensional `observations` that evaluate to true for the given constaint. Only observations the calling user has acces to are returned.\n",
                "produces": [
                    "application/json",
                    "application/x-protobuf"
                ],
                "tags": [
                    "v2"
                ],
                "parameters": [
                    {
                        "name": "assay_constraint",
                        "required": true,
                        "in": "query",
                        "description": "json that describes the assays, Example: {\"type\":\"ConceptConstraint\",\"path\":\"\\\\Public Studies\\\\CLINICAL_TRIAL_HIGHDIM\\\\High Dimensional data\\\\Expression Lung\\\\\"}",
                        "type": "string"
                    },
                    {
                        "name": "biomarker_constraint",
                        "in": "query",
                        "description": "json that describes the biomarker, Example: {\"type\":\"BiomarkerConstraint\",\"biomarkerType\":\"genes\",\"params\":{\"names\":[\"TP53\"]}}",
                        "type": "string"
                    },
                    {
                        "name": "projection",
                        "in": "query",
                        "description": "The projection Example: all_data, zscore, log_intensity",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "Dimentions are described in the `header`. The order in which they appear in the header, determens the order in which they appear in the `cells` and footer. The value in the `dimensionIndexes` corresponds to the values in the `footer`\n",
                        "schema": {
                            "$ref": "#/definitions/observations"
                        }
                    }
                }
            }
        },
        "/v2/tree_nodes": {
            "get": {
                "description": "Gets all `tree_nodes`. Number of nodes can be limited by changing the `root` path and max `depth`. `counts` and `tags` will be omitted if not requested.\n",
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
                        "description": "The node the requested tree starts from. Example: \\Public Studies\\SHARED_CONCEPTS_STUDY_A\\ "
                    },
                    {
                        "name": "depth",
                        "in": "query",
                        "type": "integer",
                        "description": "the max node depth returned"
                    },
                    {
                        "name": "counts",
                        "in": "query",
                        "type": "boolean",
                        "description": "patient and observation counts will be in the response if set to true"
                    },
                    {
                        "name": "tags",
                        "in": "query",
                        "type": "boolean",
                        "description": "tags will be in the response if set to true"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "a forist stucture if there are several root nodes. For example when there are Public Studies, Private Studies and shared concepts.\n",
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
                "description": "Gets a list of all `storage_System`\n",
                "tags": [
                    "v2"
                ],
                "responses": {
                    "200": {
                        "description": "an object that contains an array with all `storage_System`\n",
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
                "description": "Posts a new `storage_System` with the properties of the storage_System object in the body. Calling user must have `admin` premissions\n",
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
                    "200": {
                        "description": "returns a object that discribes the created `storage_System`\n",
                        "schema": {
                            "$ref": "#/definitions/storageSystem"
                        }
                    }
                }
            }
        },
        "/v2/storage/{id}": {
            "get": {
                "description": "Gets the `storage_System` with the given `id`\n",
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
                        "description": "returns a object that discribes the `storage_System`\n",
                        "schema": {
                            "$ref": "#/definitions/storageSystem"
                        }
                    }
                }
            },
            "put": {
                "description": "Replaces the `storage_System` with given id with the of the storage_System object in the body. Calling user must have `admin` premissions\n",
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
                        "description": "returns a object that discribes the modified `storage_System`\n",
                        "schema": {
                            "$ref": "#/definitions/storageSystem"
                        }
                    }
                }
            },
            "delete": {
                "description": "Deletes the `storage_System` with the given id\n",
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
                "description": "Gets a list of all `file_links`\n",
                "tags": [
                    "v2"
                ],
                "responses": {
                    "200": {
                        "description": "an object that contains an array with all `file_links`\n",
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
                "description": "Posts a new `file_link` with the properties of the file_link object in the body. Calling user must have `admin` premissions\n",
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
                    "200": {
                        "description": "returns a object that discribes the created `file_link`\n",
                        "schema": {
                            "$ref": "#/definitions/fileLink"
                        }
                    }
                }
            }
        },
        "/v2/files/{id}": {
            "get": {
                "description": "Gets the `file_link` with the given `id`\n",
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
                        "description": "returns a object that discribes the `file_link`\n",
                        "schema": {
                            "$ref": "#/definitions/fileLink"
                        }
                    }
                }
            },
            "put": {
                "description": "Replaces the `file_link` with given id with the of the file_link object in the body. Calling user must have `admin` premissions\n",
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
                        "description": "returns a object that discribes the modified `file_link`\n",
                        "schema": {
                            "$ref": "#/definitions/fileLink"
                        }
                    }
                }
            },
            "delete": {
                "description": "Deletes the `file_link` with the given id.\n",
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
        "/v2/arvados/workflows": {
            "get": {
                "description": "Gets a list of all `supported_Workflows`\n",
                "tags": [
                    "v2",
                    "arvados"
                ],
                "responses": {
                    "200": {
                        "description": "an object that contains an array with all `supported_Workflows`\n",
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
                "description": "Posts a new `supported_Workflow` with the properties of the supported_Workflow object in the body. Calling user must have `admin` premissions\n",
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
                    "200": {
                        "description": "returns a object that discribes the created `supported_Workflow`\n",
                        "schema": {
                            "$ref": "#/definitions/supportedWorkflow"
                        }
                    }
                }
            }
        },
        "/v2/arvados/workflows/{id}": {
            "get": {
                "description": "Gets the `supported_Workflow` with the given `id`\n",
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
                        "description": "returns a object that discribes the `supported_Workflow`\n",
                        "schema": {
                            "$ref": "#/definitions/supportedWorkflow"
                        }
                    }
                }
            },
            "put": {
                "description": "Replaces the `supported_Workflow` with given id with the supported_Workflow object in the body. Calling user must have `admin` premissions\n",
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
                        "description": "returns a object that discribes the modified `supported_Workflow`\n",
                        "schema": {
                            "$ref": "#/definitions/supportedWorkflow"
                        }
                    }
                }
            },
            "delete": {
                "description": "Deletes the `supportedWorkflow` with the given id.\n",
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
        "hal+jsonStudys": {
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
                    "type": "integer"
                },
                "study": {
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
                    "description": "Example: \\Public Studies\\SHARED_CONCEPTS_STUDY_A\\ "
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
                            "description": "Example: \\Public Studies\\CLINICAL_TRIAL\\Demography\\Age\\ ",
                            "type": "string"
                        }
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
                    "description": "if true, this dimension will be inlined in the cell. only pressent if true.",
                    "type": "boolean"
                },
                "fields": {
                    "description": "fields is omitted if the dimension consists of one field",
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
            "description": "numericValue or stringValue, never both",
            "properties": {
                "dimensionIndexes": {
                    "description": "the index in the array is equal to the index of the dimension in the dimensions array in the footer. The number is the index of the dimensionValue in the dimension",
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
            "description": "the structure of this value is described in the header. The order of the dimensionValues is determent by the order of the dimensionDeclaration in the header"
        }
    }
}