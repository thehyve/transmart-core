package org.transmartproject.db.querytool

/**
 * Currently only used to make grails create the table when running the
 * integration tests on a memory database.
 * All the details of this class are subject to change.
 */
class QtPatientSetCollection {

	Long            setIndex
	Long            patientNum
    QtQueryInstance resultInstance

    static belongsTo = [resultInstance: QtQueryResultInstance]

	static mapping = {
        table           schema: 'I2B2DEMODATA'
		id              column: "patient_set_coll_id", generator: "identity"

        resultInstance  column: 'result_instance_id'

		version false
	}

	static constraints = {
        resultInstance   nullable: true
		setIndex         nullable: true
		patientNum       nullable: true
	}
}
