# transmart-rest-api-e2e

## How to prepare a database

The tests are expecting specific [test data](../transmart-data/test_data) to be loaded in a database. Having additional data loaded can affect the test results, that is why it is recommended to clean all data and load only the test data before running tests.

Scripts and instructions of how to clean the database and load the test data, both for Oracle and Postgres, are a part of [transmart-data component](../transmart-data/test_data).

If using Keycloak as an external identity provider, test users needs to be added to the Keycloak database. See the instruction and the test data [here](../transmart-data/test_data/keycloak).

## How to run
These tests are designed to run against a live environment. By default it is pointed at localhost:8080

Running all tests:
```
gradle test
```

Run one test class: 
```
gradle test --tests '*ConstraintSpec'
```

Change base url:
```
gradle -DbaseUrl=http://some-transmart-server.com/ test
```

## Coverage
#### `v1`
The `v1` API is covered for backwards compatibility. Only happy cases are covered.
- `GET /studies`
- `GET /studies/{studyId}`
- `GET /studies/{studyId}/concepts`
- `GET /studies/{studyId}/concepts/{conceptPath}`
- `GET /studies/{studyId}/subjects`
- `GET /studies/{studyId}/subjects/{subjectId}`
- `GET /studies/{studyId}/concepts/{conceptPath}/subjects`
- `GET /studies/{studyId}/observations`
- `GET /observations`
- `GET /studies/{studyId}/concepts/{conceptPath}/observations`
- `POST /patient_sets`
- `GET /patient_sets/{resultInstanceId}`
- `GET /studies/{studyId}/concepts/{conceptPath}/highdim`
- `GET /studies/{studyId}/concepts/{conceptPath}/highdim?dataType={dataType}&projection={projectionType}&assayConstraints={assayConstraints}&dataConstraints={dataConstraints}`

Please note that these tests will be ignored, when using [transmart-api-server](../transmart-api-server) as a base application, since it does not support the `v1` API.

#### `v2`
The `v2` API is covered with tests for happy cases, error cases, access rights.
It also contains tests for constraints.
- `GET /observations`
- `GET /observations/aggregate`
- `GET /high_dim`
- `GET /patient_sets/$id`
- `POST /patient_sets`
- `GET /patients`
- `GET /tree_nodes`
- `GET /files`
- `GET /files/$id`
- `POST /files`
- `PUT /files/$id`
- `DELETE /files/$id`
- `GET /storage`
- `GET /storage/$id`
- `POST /storage`
- `DELETE /storage/$id`
- `PUT /storage/$id`
- `GET /arvados/workflows`
- `GET /arvados/workflows/$id`
- `POST /arvados/workflows`
- `DELETE /arvados/workflows/$id`
- `PUT /arvados/workflows/$id`
- `GET /studies`
- `GET /studies/$id`
- `GET /studies/studyId/$studyId`
- `GET /patients/$id`
- `GET /observation_list`
- `GET /supported_fields`
- `GET /studies/$studyId/files`
