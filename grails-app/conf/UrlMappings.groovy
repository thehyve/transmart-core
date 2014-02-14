class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')

        '/studies'(controller:'study', method:'GET', resources:'study', includes:['index', 'show'])

        '/studies'(resources: 'study',  method:'GET') {
            '/subjects'(controller:'subject', resources:'subject', includes:['index', 'show'])
        }
        '/studies'(resources: 'study', method:'GET') {
            '/concepts'(resources:'concept', method:'GET') {
                '/subjects'(controller:'subject', action: 'indexByConcept')
            }
        }

        '/studies'(resources: 'study',  method:'GET') {
            '/concepts'(controller:'concept', resources:'concept', includes:['index', 'show'])
        }

        '/studies'(resources: 'study',  method:'GET') {
            '/observations'(controller:'observation', resources:'observation', includes:['index'])
        }

        '/studies'(resources: 'study',  method:'GET') {
            '/concepts'(resources:'concept', method:'GET') {
                '/observations'(controller:'observation', resources:'observation', includes:['indexByConcept'])
            }
        }

        '/studies'(resources: 'study',  method:'GET') {
            '/subjects'(resources:'subject', method:'GET') {
                '/observations'(controller:'observation', resources:'observation', includes:['indexByConcept'])
            }
        }

    }
}
