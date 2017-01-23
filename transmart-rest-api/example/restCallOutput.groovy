#!/usr/bin/groovy
import groovy.json.JsonBuilder
@groovy.lang.Grapes([
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.6'),
@Grab(group = 'commons-beanutils', module = 'commons-beanutils', version = '1.8.3'),
@Grab(group = 'log4j', module = 'log4j', version = '1.2.17'),
@Grab(group = 'org.slf4j', module = 'slf4j-log4j12', version = '1.7.5'),
@Grab(group = 'org.slf4j', module = 'jcl-over-slf4j', version = '1.7.5'),
])
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

//assumes StudyTestData is used

//already the default
//PropertyConfigurator.configure('log4j.properties')

File baseExpectedFolder = null

switch (args.length) {
    case 0:
        break
    case 1:
        baseExpectedFolder = new File(args[0])
        verifyExpectedFolder(baseExpectedFolder)
        break
    default:
        println("Usage groovy restCallOutput.groovy [expectedRestOutput]")
        break
}

def jsonType = 'application/json'
def halType = 'application/hal+json'

def formats = [:]
formats.put(jsonType, 'json')
formats.put(halType, 'hal')

def urls = [:]
urls.put('/studies/STUDY1/concepts',                'concepts_per_study')
urls.put('/studies/STUDY1/concepts/bar',            'single_concept')
urls.put('/studies/study1/concepts/ROOT',           'root_concept')
urls.put('/studies/study1/subjects',                'subjects_per_study')
urls.put('/studies/study1/concepts/bar/subjects',   'subjects_per_concept')
urls.put('/studies/study1/subjects/-101',           'single_subject')

def messages = []

//File baseOutput = new File('actualRestResults')
File baseOutput = new File(File.createTempFile('dummy', '').parentFile, "actualRestResults-${System.currentTimeMillis()}")
baseOutput.mkdirs()
System.out.println("Output folder is ${baseOutput.path}")

urls.each {
    String url = it.key
    String filepart = it.value
    formats.each {
        String format = it.key
        String filename = "$filepart.${it.value}"
        File outputFile = new File(baseOutput, filename)
        outputFile.delete()
        File expectedFile = null
        if (baseExpectedFolder != null) {
            File f = new File(baseExpectedFolder, filename)
            if (f.exists()) {
                expectedFile = f
            } else {
                messages.add("Warning: expected file for $filename doesn't exist. Skipping comparison")
            }
        }
        testCall(url, format, outputFile, expectedFile, messages)
    }
}

if (messages.size() > 0) {
    messages.each { println it }
}

//does the rest call and calls the verifier
def testCall(String path, String contentType, File output, File expected, List<String> messages) {

    def tsServer = 'http://localhost:8080/'
    def ctx = 'transmart-rest-api'
    def http = new HTTPBuilder(tsServer)
    def uriPath = "$ctx$path"

    http.request(Method.GET, ContentType.JSON) {
        uri.path = "$uriPath"
        headers.Accept = contentType

        response.success = { resp, json ->
            assert resp.statusLine.statusCode == 200
            def map = [:]
            map.put('url', "$tsServer$uriPath")
            map.put('output', json)
            output << new JsonBuilder(map).toPrettyString()

            if (expected != null) {
                def slurper = new groovy.json.JsonSlurper()
                def actualRest = slurper.parseText(output.text)
                def expectedRest = slurper.parseText(expected.text)
                if (expectedRest != actualRest) {
                    messages.add("Content mismatch between ${expected.getPath()} and ${output.getPath()}")
                    //println "expected: $expectedRest"
                    //println "got: $actualRest"
                }
            }
        }

        response.failure = { resp ->
            messages.add("Failure calling $contentType on $path : ${resp.statusLine}")
            //System.err.println "Failure: $resp.statusLine"
            //System.exit 1
        }
    }
}

def verifyExpectedFolder(File file) {
    if (!file.exists()) {
        throw new IllegalArgumentException("Folder ${file.getPath()}  doesnt exist")
    }
    if (!file.isDirectory()) {
        throw new IllegalArgumentException("${file.getPath()} is not a directory")
    }
    if (!file.canRead()) {
        throw new IllegalArgumentException("Cannot read from ${file.getPath()}")
    }
}
