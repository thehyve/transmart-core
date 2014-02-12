package org.transmartproject.webservices

import geb.spock.GebReportingSpec
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class OAuthProviderTests extends GebReportingSpec {

    @Shared
    private def restUrl = "${baseUrl}"
    @Shared
    private def restClient = new RESTClient(restUrl)
    @Shared
    private def httpClient = new HTTPBuilder(restUrl)
    @Shared
    private def httpClient2 = new HTTPBuilder(restUrl)

    void "test get request without token"() {
        given:
        def resp = restClient.get(uri: restUrl, path: "studies")
        expect:
        resp.status == 200
        resp.data.toString().indexOf('Please Login') >= 0
    }

    void "test get request with invalid token"() {
        when:
        def resp = restClient.get(path: "studies", query: ['access_token': 'GARBAGE'])
        then:
        def e = thrown(groovyx.net.http.HttpResponseException)
        e.message == "Unauthorized"
    }

    void "test fetch authorization token"() {
        when: // we reuqest an authorization code
        def respCode = null
        def html = httpClient.get(path: "oauth/authorize", query: [response_type:"code", client_id:"myId", client_secret:"mySecret", redirect_uri:"http://localhost:8080/transmart-rest-api/oauth/verify"]) { resp, html ->
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
        httpClient.client.setRedirectStrategy(new org.apache.http.impl.client.DefaultRedirectStrategy() {
              @Override
              boolean isRedirected(org.apache.http.HttpRequest request, org.apache.http.HttpResponse response, org.apache.http.protocol.HttpContext context) {
                def redirected = super.isRedirected(request, response, context)
                return redirected || response.getStatusLine().getStatusCode() == 302
              }
            })
        html = httpClient.post(path:'j_spring_security_check', query: [j_username:'admin', j_password:'admin'])
        then: // we end up at the page with the authorize message
        html.text().indexOf("You hereby authorize myId") >= 0

        when: // we authorize the client
        def html2 = httpClient.post(path: "oauth/authorize", query: [response_type:"code", client_id:"myId", client_secret:"mySecret", redirect_uri:"http://localhost:8080/transmart-rest-api/oauth/verify"], body: [authorize:"Authorize", user_oauth_approval:true]) { resp, htmlt ->
                respCode = resp.statusLine
                println "htmlT:${htmlt}"
                return htmlt
        }
        then: // we receive the code that was generated
        respCode?.statusCode == 200
        html2.text().length() >= 5
        def codeShort = html2.text()
        println "code:${html2.text()}."

        when:
        def html3 = httpClient2.post(path: "oauth/token", query: [grant_type:"authorization_code", client_id:"myId", client_secret:"mySecret", redirect_uri:"http://localhost:8080/transmart-rest-api/oauth/verify", code:codeShort], body:[code:codeShort]) { resp, htmlr ->
                respCode = resp.statusLine
                println "htmlR:${htmlr}"
                return htmlr
        }
        then:
        println "token result:${html3}"
        respCode?.statusCode == 200
        html3.size() > 0
        html3.access_token != null
        def accessToken = html3.access_token
        accessToken != null
        accessToken.size() >= 16
    }

    // void "test fetch token"() {
    //     given:
    //     def resp = restClient.get(uri: restUrl, path: "oauth/token", query: ["grant_type":"client_credentials", "client_id":"myId", "client_secret":"mySecret"])
    //     expect:
    //     resp.status == 200
    //     resp.data.expires_in > 40000
    //     resp.data.token_type == "bearer"
    //     resp.data.access_token.size() > 16

    // }

    // void "test unauthorized authorize call"() {
    //     when:
    //     def resp = restClient.get(uri: restUrl, path: 'oAuth/authorize', query:['response_type':'code', 'client_id':'myId', 'redirect_uri':restUrl])
    //     then:
    //     thrown(groovyx.net.http.HttpResponseException)
    // }


    // test mapping of rest calls
    // test with and without end slash
    // test with .json, etc
    // test with access token in header
}
