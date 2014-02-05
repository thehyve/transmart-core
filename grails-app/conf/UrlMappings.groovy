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
            '/observations'(controller:'observation', resources:'observation', includes:['index', 'show'])
        }
        '/studies'(resources: 'study',  method:'GET') {
            '/subjects'(controller:'subject', resources:'subject', includes:['index', 'show'])
        }

	}
}
