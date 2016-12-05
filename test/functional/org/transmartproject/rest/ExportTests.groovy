package org.transmartproject.rest

import org.codehaus.groovy.grails.web.mime.MimeType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ExportTests extends ResourceTestCase{

    String version = "v1"


    private static final String JSON_CONCEPTS_DATATYPES_URI = "%5B%7B%22conceptKeys%22%3A+%5B%22%5C%5C%5C%5Ci2b2+main%5C%5Cfoo%5C%5Cstudy1%5C%5Cbar%5C%5C%22%5D%7D%2C+%7B%22conceptKeys%22%3A+%5B%22%5C%5C%5C%5Ci2b2+main%5C%5Cfoo%5C%5Cstudy2%5C%5Clong+path%5C%5C%22%2C+%22%5C%5C%5C%5Ci2b2+main%5C%5Cfoo%5C%5Cstudy2%5C%5Csex%5C%5C%22%5D%7D%5D"

    void testDataTypes() {
        get("${baseURL}$version/export/datatypes", { concepts=JSON_CONCEPTS_DATATYPES_URI })
        assertStatus 200
        //JSON -> Map etc structure, compare it to MAP structure
        assertThat( JSON, allOf(contains(
                allOf(
                        hasEntry('dataType', 'Messenger RNA data (Microarray)'),
                        hasEntry('dataTypeCode', 'mrna'),
                        hasEntry(is('cohorts'), contains(allOf(
                                hasEntry(is('concepts'), contains(allOf(
                                        hasEntry('numOfPatients', 0),
                                        hasEntry('conceptPath', "\\foo\\study1\\bar\\")
                                )))
                        ))),
                ),
                allOf(
                        hasEntry('dataType', 'Clinical data'),
                        hasEntry('dataTypeCode', 'clinical'),
                        hasEntry(is('cohorts'), contains(allOf(
                                hasEntry(is('concepts'), allOf(
                                        contains(
                                                allOf(
                                                        hasEntry('numOfPatients', 0),
                                                        hasEntry('conceptPath', "\\foo\\study2\\long path\\")
                                                ),
                                                allOf(
                                                        hasEntry('numOfPatients', 0),
                                                        hasEntry('conceptPath', "\\foo\\study2\\sex\\")
                                                ))
                                ))
                        ))),
                )
        )))
    }

}
