#transmart-rest-api-e2e
##how to run
These tests are designed to run against a live environment. By default it is pointed at localhost:8080

Running all tests:
>gradle test

Run one test class: 
>gradle test --tests '*ConstraintSpec'

Change base url:
>gradle -DbaseUrl=http://some-transmart-server.com/ test

##Coverage
####V1
The v1 api is covered for backwards compatibility. Only happy cases are covered.
- GET /studies
- GET /studies/{studyId}
- GET /studies/{studyId}/concepts
- GET /studies/{studyId}/concepts/{conceptPath}
- GET /studies/{studyId}/subjects
- GET /studies/{studyId}/subjects/{subjectId} 
- GET /studies/{studyId}/concepts/{conceptPath}/subjects
- GET /studies/{studyId}/observations
- GET /observations
- GET /studies/{studyId}/concepts/{conceptPath}/observations
- POST /patient_sets
- GET /patient_sets/{resultInstanceId}
- GET /studies/{studyId}/concepts/{conceptPath}/highdim
- GET /studies/{studyId}/concepts/{conceptPath}/highdim?dataType={dataType}&projection={projectionType}&assayConstraints={assayConstraints}&dataConstraints={dataConstraints}

####V2
The v2 api is covered with tests for happy cases, error cases, access rights.
It also contains tests for constants.
- GET /observations
- GET /observations/aggregate
- GET /high_dim
- GET /patient_sets/$id
- POST /patient_sets
- GET /patients
- GET /tree_nodes
- GET /files
- GET /files/$id
- POST /files
- PUT /files/$id
- DELETE /files/$id
- GET /storage
- GET /storage/$id
- POST /storage
- DELETE /storage/$id
- PUT /storage/$id
- GET /arvados/workflows
- GET /arvados/workflows/$id
- POST /arvados/workflows
- DELETE /arvados/workflows/$id
- PUT /arvados/workflows/$id
- GET /studies
- GET /studies/$id
- GET /studies/studyId/$studyId
- GET /observations/count
- GET /patients/$id
- GET /observation_list
- GET /supported_fields
- GET /studies/$studyId/files