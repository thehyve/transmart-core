package org.transmartproject.db.user

class SearchAuthPrincipal {

    String  principalType
    String  description
    String  name
    String  uniqueId
    Boolean enabled

    static mapping = {
        table   schema:    'searchapp'
		id      generator: 'assigned'

        autoTimestamp true
		version false
	}

	static constraints = {
        principalType nullable: true
        description   nullable: true
        name          nullable: true
        uniqueId      nullable: true
        enabled       nullable: true
	}
}
