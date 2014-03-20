class RestApiUrlMappings {

    static mappings = {
        '/studies'(controller: 'study', method: 'GET', resources: 'study', includes: ['index', 'show'])

        '/studies'(resources: 'study', method: 'GET') {
            '/subjects'(controller: 'subject', resources: 'subject', includes: ['index', 'show'])
        }

        "/studies/$studyId/concepts"(
                controller: 'concept', action: 'index'
        )

        "/studies/$studyId/concepts/$id**"(
                controller: 'concept', action: 'show'
        )

        "/studies/$studyId/concepts/$conceptId**/subjects"(
                controller: 'subject', action: 'indexByConcept'
        )

        "/studies/$studyId/concepts/$conceptId**/observations"(
                controller: 'observation', action: 'indexByConcept'
        )

        '/studies'(resources: 'study', method: 'GET') {
            '/observations'(controller: 'observation', resources: 'observation', includes: ['index'])
        }

        '/studies'(resources: 'study', method: 'GET') {
            '/subjects'(resources: 'subject', method: 'GET') {
                '/observations'(controller: 'observation', action:'indexBySubject')
            }
        }

    }
}
