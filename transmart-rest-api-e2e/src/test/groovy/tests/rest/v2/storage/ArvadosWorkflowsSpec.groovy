/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.storage

import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.ADMIN_USER
import static config.Config.PATH_ARVADOS_WORKFLOWS

/**
 *  Creating ArvadosWorkflows
 *  TMPREQ-31 Starting an Arvados workflow from tranSMART API.
 */
class ArvadosWorkflowsSpec extends RESTSpec {


    def setup() {
        def responseDataAll = get([path: PATH_ARVADOS_WORKFLOWS, acceptType: JSON, user: ADMIN_USER])
        responseDataAll.supportedWorkflows.each {
            delete([path: PATH_ARVADOS_WORKFLOWS + "/${it.id}", acceptType: JSON, statusCode: 204, user: ADMIN_USER])
        }
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"() {
        given:
        def data = ["uuid"              : "bla",
                    "arvadosInstanceUrl": "a",
                    "name"              : "name",
                    "description"       : "sc",
                    "arvadosVersion"    : "v1",
                    "defaultParams"     : [
                            "a": 1, "b": "b"
                    ],
        ]

        when:
        def responseData = post([
                path      : PATH_ARVADOS_WORKFLOWS,
                acceptType: JSON,
                body      : toJSON(data),
                statusCode: 201,
                user      : ADMIN_USER
        ])
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        responseData = get([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: JSON, user: ADMIN_USER])

        then:
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        def responseDataAll = get([path: PATH_ARVADOS_WORKFLOWS, acceptType: JSON, user: ADMIN_USER])

        then:
        assert responseDataAll.supportedWorkflows.contains(responseData)

        when:
        data.name = 'new file Link renamed'
        responseData = put([path: PATH_ARVADOS_WORKFLOWS + "/${id}", body: toJSON(data), user: ADMIN_USER])

        then:
        assert responseData.id == id
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        responseData = delete([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: JSON, statusCode: 204, user: ADMIN_USER])
        assert responseData == null
        responseData = get([path: PATH_ARVADOS_WORKFLOWS + "/${id}", acceptType: JSON, statusCode: 404, user: ADMIN_USER])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "${PATH_ARVADOS_WORKFLOWS}/${id}"
    }

    /**
     *  post invalid
     */
    //TODO: could do with a better error
    def "post invalid values"() {
        given:
        def request = [
                path      : PATH_ARVADOS_WORKFLOWS,
                acceptType: JSON,
                body      : toJSON(["uuid"              : null,
                                    "arvadosInstanceUrl": null,
                                    "name"              : null,
                                    "description"       : null,
                                    "arvadosVersion"    : null,
                                    "defaultParams"     : null
                ]),
                statusCode: 500,
                user      : ADMIN_USER
        ]

        when:
        def responseData = post(request)

        then:
        assert responseData.httpStatus == 500
//        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.ArvadosController'
//        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  post empty
     */
    //TODO: could do with a better error
    def "post empty"() {
        when:
        def responseData = post([
                path      : PATH_ARVADOS_WORKFLOWS,
                acceptType: JSON,
                body      : null,
                statusCode: 500,
                user      : ADMIN_USER
        ])

        then:
        assert responseData.httpStatus == 500
//        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.ArvadosController'
//        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"() {
        when:
        def responseData = get([
                path      : PATH_ARVADOS_WORKFLOWS + "/0",
                acceptType: JSON,
                statusCode: 404,
                user      : ADMIN_USER
        ])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "${PATH_ARVADOS_WORKFLOWS}/0"
    }

    /**
     *  put invalid
     */
    def "put invalid"() {
        given:
        def data = ["uuid"              : "bla",
                    "arvadosInstanceUrl": "a",
                    "name"              : "name",
                    "description"       : "sc",
                    "arvadosVersion"    : "v1",
                    "defaultParams"     : [
                            "a": 1, "b": "b"
                    ],
        ]
        def responseData = post([
                path      : PATH_ARVADOS_WORKFLOWS,
                acceptType: JSON,
                body      : toJSON(data),
                statusCode: 201,
                user      : ADMIN_USER
        ])
        data.uuid = null

        when:
        responseData = put([
                path      : PATH_ARVADOS_WORKFLOWS + "/${responseData.id}",
                body      : toJSON(data),
                statusCode: 422,
                user      : ADMIN_USER
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
        def data = ["uuid"              : "bla",
                    "arvadosInstanceUrl": "a",
                    "name"              : "name",
                    "description"       : "sc",
                    "arvadosVersion"    : "v1",
                    "defaultParams"     : [
                            "a": 1, "b": "b"
                    ],
        ]

        when:
        def responseData = put([
                path      : PATH_ARVADOS_WORKFLOWS + "/0",
                acceptType: JSON,
                body      : toJSON(data),
                statusCode: 404,
                user      : ADMIN_USER
        ])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "${PATH_ARVADOS_WORKFLOWS}/0"
    }

    /**
     *  no access
     */
    def "no access"() {
        given:
        def data = ["uuid"              : "bla",
                    "arvadosInstanceUrl": "a",
                    "name"              : "name",
                    "description"       : "sc",
                    "arvadosVersion"    : "v1",
                    "defaultParams"     : [
                            "a": 1, "b": "b"
                    ],
        ]

        when:
        def responseData = post([
                path      : PATH_ARVADOS_WORKFLOWS,
                acceptType: JSON,
                body      : toJSON(data),
                statusCode: 403,
        ])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating a new supported workflowis an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        data.name = 'new file Link renamed'
        responseData = put([
                path      : PATH_ARVADOS_WORKFLOWS + "/0",
                acceptType: JSON,
                body      : toJSON(data),
                statusCode: 403,
        ])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Modifying a supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        responseData = delete([path: PATH_ARVADOS_WORKFLOWS + "/0", statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a new supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }
}
