package tests.rest.v2.storage

import base.RESTSpec
import spock.lang.IgnoreIf

import static config.Config.*

class ArvadosWorkflowsSpec extends RESTSpec{


    def setup() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get(PATH_ARVADOS_WORKFLOWS)
        responseDataAll.supportedWorkflows.each{
            delete(PATH_ARVADOS_WORKFLOWS + "/${it.id}")
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

        when:
        def responseData = post(PATH_ARVADOS_WORKFLOWS, toJSON(data))
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        responseData = get(PATH_ARVADOS_WORKFLOWS + "/${id}")

        then:
        assert responseData.name == data.name
        assert responseData.arvadosInstanceUrl == data.arvadosInstanceUrl
        assert responseData.defaultParams == data.defaultParams
        assert responseData.description == data.description
        assert responseData.uuid == data.uuid

        when:
        def responseDataAll = get(PATH_ARVADOS_WORKFLOWS)

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
        responseData = delete(PATH_ARVADOS_WORKFLOWS + "/${id}")
        assert responseData == null
        responseData = get(PATH_ARVADOS_WORKFLOWS + "/${id}")

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
        def data = ["uuid": null,
                    "arvadosInstanceUrl": null,
                    "name": null,
                    "description": null,
                    "arvadosVersion": null,
                    "defaultParams": null
        ]

        when:
        def responseData = post(PATH_ARVADOS_WORKFLOWS, toJSON(data))

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
        def responseData = post(PATH_ARVADOS_WORKFLOWS, null)

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
        def responseData = get(PATH_ARVADOS_WORKFLOWS + "/0")

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
        def responseData = post(PATH_ARVADOS_WORKFLOWS, toJSON(data))
        def id = responseData.id
        data.uuid = null

        when:
        responseData = put(PATH_ARVADOS_WORKFLOWS +"/${id}", toJSON(data))

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
        def responseData = put(PATH_ARVADOS_WORKFLOWS +"/0", toJSON(data))

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_ARVADOS_WORKFLOWS}/0"
    }

    /**
     *  no access
     */
    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) // "Removing a 'new' supported workflow entry ??"
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
        def responseData = post(PATH_ARVADOS_WORKFLOWS, toJSON(data))

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating a new supported workflowis an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        data.name = 'new file Link renamed'
        responseData = put(PATH_ARVADOS_WORKFLOWS + "/0", toJSON(data))

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Modifying a supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        responseData = delete(PATH_ARVADOS_WORKFLOWS + "/0")

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a supported workflow entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }
}
