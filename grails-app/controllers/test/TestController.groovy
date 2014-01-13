package test

import org.scribe.model.Token
import org.scribe.model.Verifier
import uk.co.desirableobjects.oauth.scribe.OauthService
import grails.plugin.springsecurity.annotation.Secured

class TestController {
    OauthService oauthService
    private static final Token EMPTY_TOKEN = new Token('', '')

    @Secured('permitAll')
	def test() {
		render "hoi"
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