package test

import org.scribe.model.Token
import org.scribe.model.Verifier
import uk.co.desirableobjects.oauth.scribe.OauthService
import grails.plugin.springsecurity.annotation.Secured
import grails.converters.JSON
import org.transmartproject.webservices.Study
import org.transmartproject.webservices.Observation

class TestController {
    OauthService oauthService
    private static final Token EMPTY_TOKEN = new Token('', '')

    @Secured('permitAll')
	def test() {
        // Study study1 = new Study(name:"yeah")
        // Study study2 = new Study(name:"ohno")
        // Study study3 = new Study(name:"ole!")
        // List list = [study1,study2,study3]
        Observation obs1 = new Observation(name:"yeah", id:1)
        Observation obs2 = new Observation(name:"yeah 2", id:2)
        Observation obs3 = new Observation(name:"yeah 3", id:3)
        List list = [obs1,obs2,obs3]
        render list as JSON
	}

    @Secured('permitAll')
    def index() {

    }

    @Secured('permitAll')
    def verify() {
        Verifier verifier = new Verifier(params.code)
        Token accessToken = oauthService.getMineAccessToken(EMPTY_TOKEN, verifier)
        render text:oauthService.getMineResource(accessToken, 'http://localhost:8080/transmart-rest-api/studies/').body, contentType:"application/json"
    }
    @Secured('permitAll')
    def verify2() {
        render params.code
    }
}