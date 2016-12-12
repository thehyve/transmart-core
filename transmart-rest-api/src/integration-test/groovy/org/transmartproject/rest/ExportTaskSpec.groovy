package org.transmartproject.rest

import org.grails.web.json.JSONArray

class ExportTaskSpec extends ResourceSpec {

    private static final String VERSION = "/v1"

    private static
    final String JSON_CONCEPTS_DATATYPES_URI = "%5B%7B%22conceptKeys%22%3A%20%5B%22%5C%5C%5C%5Ci2b2%20main%5C%5Cfoo%5C%5Cstudy1%5C%5Cbar%5C%5C%22%5D%7D%2C%20%7B%22conceptKeys%22%3A%20%5B%22%5C%5C%5C%5Ci2b2%20main%5C%5Cfoo%5C%5Cstudy2%5C%5Clong%20path%5C%5C%22%2C%20%22%5C%5C%5C%5Ci2b2%20main%5C%5Cfoo%5C%5Cstudy2%5C%5Csex%5C%5C%22%5D%7D%5D%0A"

    void testDataTypes() {
        when:
        def response = get "$VERSION/export/datatypes?concepts=$JSON_CONCEPTS_DATATYPES_URI"
        then:
        response.status == 200
        //JSON -> Map etc structure, compare it to MAP structure
        assert response.json == getDataTypesList()
    }

    JSONArray getDataTypesList() {
        JSONArray returns = [getMrnaMap(), getClinicalMap()]
    }

    def getMrnaMap() {
        def mrnaMap = [
                "dataTypeCode": "mrna",
                "dataType"    : "Messenger RNA data (Microarray)",
                "cohorts"     : [[
                                         "concepts": [[
                                                              "subjects"   : [-101],
                                                              "conceptPath": "\\foo\\study1\\bar\\"
                                                      ]]
                                 ]]]
    }

    def getClinicalMap() {
        def clinicalMap = [
                "dataTypeCode": "clinical",
                "dataType"    : "Clinical data",
                "cohorts"     : [
                        ["concepts": [[
                                              "subjects"   : [],
                                              "conceptPath": "\\foo\\study2\\long path\\"
                                      ],[
                                              "subjects"   : [-202, -201],
                                              "conceptPath": "\\foo\\study2\\sex\\"
                                      ]]
                        ]]]
    }

}
