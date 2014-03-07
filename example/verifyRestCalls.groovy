#!/usr/bin/groovy

import groovy.json.JsonBuilder
@groovy.lang.Grapes([
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6' ),
    @Grab(group='commons-beanutils', module='commons-beanutils', version='1.8.3'),
    @Grab(group='log4j', module='log4j', version='1.2.17'),
    @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.5'),
    @Grab(group='org.slf4j', module='jcl-over-slf4j', version='1.7.5'),
])
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.HttpResponse
import org.apache.http.impl.client.BasicCookieStore
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator

//validates the rest-api results, assuming StudyTestData is used

//already the default
//PropertyConfigurator.configure('log4j.properties')

def jsonType = 'application/json'
def halType = 'application/hal+json'

def fooConceptJsonVerifier = { json ->
    assert json.size() == 3
    assert json.name == 'bar'
    assert json.fullName == '\\foo\\study1\\bar\\'
    assert json.key == '\\\\i2b2 main\\foo\\study1\\bar\\'
}

def fooConceptHalVerifier = { json ->
    assert json.size() == 4
    assert json.name == 'bar'
    assert json.fullName == '\\foo\\study1\\bar\\'
    assert json.key == '\\\\i2b2 main\\foo\\study1\\bar\\'
    assert json._links?.self?.href ==  '/studies/study1/concepts/bar'
}

def conceptsUrl = "/studies/STUDY1/concepts"

testCall(conceptsUrl, jsonType,
        { json ->
            def list = json.ontology_terms
            assert list?.size() == 1
            fooConceptJsonVerifier.call(list[0])
        }
)

testCall(conceptsUrl, halType,
        { json ->
            assert json.size() == 2
            assert json._links?.self?.href == '/studies/study1/concepts'
            def list = json._embedded?.ontology_terms
            assert list?.size() == 1
            fooConceptHalVerifier.call(list[0])
        }
)

def conceptBarUrl = "/studies/STUDY1/concepts/bar"

testCall(conceptBarUrl, jsonType, fooConceptJsonVerifier)

testCall(conceptBarUrl, halType, fooConceptHalVerifier)

def studyRootConceptUrl = '/studies/study1/concepts/ROOT'
testCall(studyRootConceptUrl, jsonType,
        { json ->
            assert json.size() == 3
            assert json.name == 'study1'
            assert json.fullName == '\\foo\\study1\\'
            assert json.key == '\\\\i2b2 main\\foo\\study1\\'

        }
)

testCall(studyRootConceptUrl, halType,
        { json ->
            assert json.size() == 4
            assert json.name == 'study1'
            assert json._links?.self?.href == studyRootConceptUrl
            assert json.fullName == '\\foo\\study1\\'
            assert json.key == '\\\\i2b2 main\\foo\\study1\\'
        }
)

//does the rest call and calls the verifier
def testCall(String path, String contentType, Closure verifier) {

    def tsServer = 'http://localhost:8080/'
    def ctx = 'transmart-rest-api'
    def http = new HTTPBuilder(tsServer)

    http.request(Method.GET, ContentType.JSON) {
        uri.path = "$ctx/$path"
        headers.Accept = contentType

        response.success = { resp, json ->
            assert resp.statusLine.statusCode == 200
            verifier.call(json)
        }

        response.failure = { resp ->
            System.err.println "Failure: $resp.statusLine"
            System.exit 1
        }
    }
}
