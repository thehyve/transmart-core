package mock.ontology.server

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {}
        }

        "/search/$conceptCode"(controller: 'ontologyTerm', action:'index')
        "/$roxId"(controller: 'ontologyTerm', action:'show')
        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
