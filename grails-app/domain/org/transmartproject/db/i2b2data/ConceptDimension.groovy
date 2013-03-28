package org.transmartproject.db.i2b2data

/**
 * Currently only used to make grails create the table when running the
 * integration tests on a memory database.
 * All the details of this class are subject to change.
 */
class ConceptDimension {

    String       conceptPath
    String       conceptCd
    String       nameChar
    String       conceptBlob
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    BigDecimal   uploadId

	static mapping = {
        table   schema: 'I2B2DEMODATA'
		id      name: "conceptPath", generator: "assigned"
		version false
	}

	static constraints = {
        conceptPath      maxSize:    700
        conceptCd        maxSize:    50
        nameChar         nullable:   true,   maxSize:   2000
        conceptBlob      nullable:   true
        updateDate       nullable:   true
        downloadDate     nullable:   true
        importDate       nullable:   true
        sourcesystemCd   nullable:   true,   maxSize:   50
        uploadId         nullable:   true
	}
}
