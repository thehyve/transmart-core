package org.transmartproject.webservices

import spock.lang.*
import pages.*
import geb.spock.GebReportingSpec
import groovyx.net.http.RESTClient
import groovyx.net.http.HTTPBuilder
import pages.login.LoginPage
import pages.login.DeniedPage

@Stepwise
class OAuthProviderTests extends GebReportingSpec {

    @Shared
    private def restUrl = "${baseUrl}"
    @Shared
    private def restClient = new RESTClient(restUrl)
    @Shared
    private def httpClient = new HTTPBuilder(restUrl)

    void "test get request without token"() {
        given:
        def resp = restClient.get(uri: restUrl, path: "studies")
        // resp.headers.each { h ->
        //     println " ${h.name} : ${h.value}"
        // }
        expect:
        // assert resp.status == 401
        // TODO DvM: Hmm, somehow spring does not respond with a 401, but instead with a 302 pointing to the login page
        resp.status == 200
        resp.data.toString().indexOf('Please Login') >= 0
    }

    void "test get request with invalid token"() {
        when:
        def resp = restClient.get(path: "studies", query: ['access_token': 'GARBAGE'])
        then:
        def e = thrown(groovyx.net.http.HttpResponseException)
        e.message == "Unauthorized"
        // resp.data.toString().indexOf('error: "invalid_token"') >= 0
    }

    void "test fetch authorization token"() {
        when: // we reuqest an authorization code
        def respCode = null
        def html = httpClient.get(path: "oauth/authorize", query: [response_type:"code", client_id:"myId", client_secret:"mySecret", redirect_uri:"http://localhost:8080/transmart-rest-api/test/verify2"]) { resp, html ->
                respCode = resp.statusLine
                return html
        }
        then: // we end up on the login page
        respCode != null
        respCode.statusCode ==200
        html.toString().indexOf('Login') >= 0
        println "form submit:${html.body.toString().indexOf('j_spring_security_check')}"
        // html.body.toString().indexOf('j_spring_security_check') >= 0

        when: // we log in with the test account
        // loginForm.j_username = 'bob'
        // loginForm.j_password = 'pass'
        // loginButton.click()
        httpClient.client.setRedirectStrategy(new org.apache.http.impl.client.DefaultRedirectStrategy() {
              @Override
              boolean isRedirected(org.apache.http.HttpRequest request, org.apache.http.HttpResponse response, org.apache.http.protocol.HttpContext context) {
                def redirected = super.isRedirected(request, response, context)
                return redirected || response.getStatusLine().getStatusCode() == 302
              }
            })
        html = httpClient.post(path:'j_spring_security_check', query: [j_username:'bob', j_password:'pass'])
        then: // we end up at the page with the authorize message
        html.text().indexOf("You hereby authorize myId") >= 0

        when: // we authorize the client
        html = httpClient.get(path: "oauth/authorize", query: [response_type:"code", client_id:"myId", client_secret:"mySecret", redirect_uri:"http://localhost:8080/transmart-rest-api/test/verify2", authorize:"Authorize", user_oauth_approval:true]) { resp, htmlt ->
                respCode = resp.statusLine
                return htmlt
        }
        then: // we receive the code that was generated
        println "data:${html}"
        respCode?.statusCode == 200
        html.body.text().indexOf('code:') >= 0
        println "code:${html.body.code}"
        html.body.code.size() > 4

        when:
        resp = restClient.get(path: "oauth/authorize", query: ["response_type":"code"])
        then:
        println resp.data
        resp.status == 200
        resp.data.size() > 0
    }

    void "test fetch token"() {
        given:
        def resp = restClient.get(uri: restUrl, path: "oauth/token", query: ["grant_type":"client_credentials", "client_id":"myId", "client_secret":"mySecret"])
        expect:
        resp.status == 200
        resp.data.expires_in > 40000
        resp.data.token_type == "bearer"
        resp.data.access_token.size() > 16

    }

    // void "test unauthorized authorize call"() {
    //     when:
    //     def resp = restClient.get(uri: restUrl, path: 'oAuth/authorize', query:['response_type':'code', 'client_id':'myId', 'redirect_uri':restUrl])
    //     then:
    //     thrown(groovyx.net.http.HttpResponseException)
    // }

    void "test fetch token and authorize"() {
        when:
        def resp = restClient.get(uri: restUrl, path: 'oauth/token', query: ['grant_type':'client_credentials', 'client_id':'myId', 'client_secret':'mySecret'])

        then:
        assert resp.status == 200
        assert resp.data.expires_in > 40000
        assert resp.data.token_type == "bearer"
        assert resp.data.access_token.size() > 16
        def accessToken = resp.data.access_token
        println "access token:$accessToken"
        when:
        resp = restClient.get(uri: restUrl, path: 'oAuth/authorize', query:['response_type':'code', 'client_id':'myId', 'redirect_uri':restUrl, 'access_token':accessToken])
        then:
        println resp.data
        assert resp.status == 200
        assert resp.data.expires_in > 40000
        assert resp.data.token_type == "bearer"
        assert resp.data.access_token.size() > 16
    }
    // test mapping of rest calls
    // test with and without end slash
    // test with .json, etc
    // test with access token in header
}
