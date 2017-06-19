class UrlMappings {
    /**
     * This mappings file seems to be required when inlining core-db plugins
     * (as stated here https://github.com/transmart/transmart-rest-api/blob/v1.2.4/grails-app/conf/UrlMappings.groovy).
     * Without it, we get "Class not found loading Grails application: UrlMappings"
     * (https://travis-ci.org/transmart/SmartR/builds/163942859)
     */

	static mappings = {
        // Following block seems to be required by functional tests (BaseAPITestCase.groovy)
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        // Removed the two following lines in order to not interfere with transmartApp url mappings - TRANSREL-134
        //"/"(view:"/index")
        //"500"(view:'/error')
	}
}
