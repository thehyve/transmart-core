package tests.rest.v2.storage

import base.RESTSpec
import spock.lang.IgnoreIf

import static config.Config.*
import static tests.rest.v2.constraints.ConceptConstraint
import static tests.rest.v2.constraints.PatientSetConstraint

/**
 *  Creating ArvadosWorkflows
 *  TMPREQ-31 Starting an Arvados workflow from tranSMART API.
 */
class ArvadosWorkflowsSpec extends RESTSpec{


    def setup() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get([path: PATH_ARVADOS_WORKFLOWS, acceptType: contentTypeForJSON])
        responseDataAll.supportedWorkflows.each{
            delete([path: PATH_ARVADOS_WORKFLOWS + "/${it.id}", acceptType: contentTypeForJSON])
        }
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def data = ["uuid":"bla",
                    "arvadosInstanceUrl":"a",
                    "name":"name",
                    "description":"sc",
                    "arvadosVersion":"v1",
                    "defaultParams": [
                            "a":1, "b":"b"
                    ],
        ]

        def request = [
                path: PATH_ARVADOS_WORKFLOWS,
                acceptType: contentTypeForJSON,
                body: toJSON(data)
        ]

        when:
        def responseData = post(request)
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        responseData = get([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: contentTypeForJSON])

        then:
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        def responseDataAll = get([path: PATH_ARVADOS_WORKFLOWS, acceptType: contentTypeForJSON])

        then:
        assert responseDataAll.supportedWorkflows.contains(responseData)

        when:
        data.name = 'new file Link renamed'
        responseData = put(PATH_ARVADOS_WORKFLOWS + "/${id}", toJSON(data))

        then:
        assert responseData.id == id
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        responseData = delete([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: contentTypeForJSON])
        assert responseData == null
        responseData = get([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_ARVADOS_WORKFLOWS}/${id}"
    }

    /**
     *  post invalid
     */
    def "post invalid values"() {
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def request = [
                path: PATH_ARVADOS_WORKFLOWS,
                acceptType: contentTypeForJSON,
                body: toJSON(["uuid": null,
                              "arvadosInstanceUrl": null,
                              "name": null,
                              "description": null,
                              "arvadosVersion": null,
                              "defaultParams": null
                ]),
                statusCode: 500
        ]

        when:
        def responseData = post(request)

        then:
        assert responseData.httpStatus == 500
        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.ArvadosController'
        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  post empty
     */
    def "post empty"() {
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)

        when:
        def responseData = post([
                path: PATH_ARVADOS_WORKFLOWS,
                acceptType: contentTypeForJSON,
                body: null,
                statusCode: 500
        ])

        then:
        assert responseData.httpStatus == 500
        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.ArvadosController'
        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"() {
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)

        when:
        def responseData = get([
                path: PATH_ARVADOS_WORKFLOWS + "/0",
                acceptType: contentTypeForJSON,
                statusCode: 404
        ])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_ARVADOS_WORKFLOWS}/0"
    }

    /**
     *  put invalid
     */
    def "put invalid"() {
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def data = ["uuid":"bla",
                    "arvadosInstanceUrl":"a",
                    "name":"name",
                    "description":"sc",
                    "arvadosVersion":"v1",
                    "defaultParams": [
                            "a":1, "b":"b"
                    ],
        ]
        def responseData = post([
                path: PATH_ARVADOS_WORKFLOWS,
                acceptType: contentTypeForJSON,
                body: toJSON(data)

        ])
        data.uuid = null

        when:
        responseData = put([
                path: PATH_ARVADOS_WORKFLOWS +"/${responseData.id}",
                body: toJSON(data),
                statusCode: 422
        ])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'uuid'
        assert responseData.errors[0].message == 'Property [uuid] of class [class org.transmartproject.db.arvados.SupportedWorkflow] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
        assert responseData.errors[0].object == 'org.transmartproject.db.arvados.SupportedWorkflow'
    }

    /**
     *  put nonexistent
     */
    def "put nonexistent"() {
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def data = ["uuid":"bla",
                    "arvadosInstanceUrl":"a",
                    "name":"name",
                    "description":"sc",
                    "arvadosVersion":"v1",
                    "defaultParams": [
                            "a":1, "b":"b"
                    ],
        ]

        when:
        def responseData = put([
                path: PATH_ARVADOS_WORKFLOWS +"/0",
                acceptType: contentTypeForJSON,
                body: toJSON(data),
                statusCode: 404
        ])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_ARVADOS_WORKFLOWS}/0"
    }

    /**
     *  no access
     */
    def "no access"(){
        given:
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def data = ["uuid":"bla",
                    "arvadosInstanceUrl":"a",
                    "name":"name",
                    "description":"sc",
                    "arvadosVersion":"v1",
                    "defaultParams": [
                            "a":1, "b":"b"
                    ],
        ]

        when:
        def responseData = post([
                path: PATH_ARVADOS_WORKFLOWS,
                acceptType: contentTypeForJSON,
                body: toJSON(data),
                statusCode: 403
        ])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating a new supported workflowis an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        data.name = 'new file Link renamed'
        responseData = put([
                path: PATH_ARVADOS_WORKFLOWS + "/0",
                acceptType: contentTypeForJSON,
                body: toJSON(data),
                statusCode: 403
        ])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Modifying a supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        responseData = delete(PATH_ARVADOS_WORKFLOWS + "/0")

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a new supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }
}
