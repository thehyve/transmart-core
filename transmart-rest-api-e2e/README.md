# transmart-rest-api-e2e
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
- `GET /observations/count`
- `GET /patients/$id`
- `GET /observation_list`
- `GET /supported_fields`
- `GET /studies/$studyId/files`


## License
The transmart-rest-api-e2e module is copyright 2017 The Hyve B.V.

The transmart-rest-api-e2e module is licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
