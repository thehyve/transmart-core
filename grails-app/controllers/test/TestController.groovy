package test

class TestController {
 //    OauthService oauthService
 //    ConceptsResource conceptsResourceService
 //    grails.plugin.springsecurity.SpringSecurityService springSecurityService

 //    private static final Token EMPTY_TOKEN = new Token('', '')

 //    @Secured('permitAll')
 //    def studies() {
 //        render conceptsResourceService.getAllStudies() as JSON
	// }

 //    @Secured('permitAll')
 //    def index() {
 //        render "hash:${springSecurityService.encodePassword('admin')}"
 //    }

 //    @Secured('permitAll')
 //    def verify() {
 //        Verifier verifier = new Verifier(params.code)
 //        Token accessToken = oauthService.getMineAccessToken(EMPTY_TOKEN, verifier)
 //        render text:oauthService.getMineResource(accessToken, 'http://localhost:8080/transmart-rest-api/studies/').body, contentType:"application/json"
 //    }

 //    @Secured('permitAll')
 //    def verify2() {
 //        def returnObject = [:]
 //        returnObject.code = params.code
 //        render returnObject as JSON
 //    }

 //    @Secured('permitAll')
 //    def testje() {
 //        def conceptcodes = ConceptDimension.withCriteria {
 //            like 'conceptPath', params.path + '%'
 //        }.collect { it -> it.conceptCode }
 //        log.info "conceptcodes:$conceptcodes"
 //        def output =
 //        PatientDimension.withCriteria {
 //            assays {
 //                'in' 'conceptCode', conceptcodes
 //            }
 //        }
 //        log.info "output =$output"
 //        render output as JSON
 //    }
}
