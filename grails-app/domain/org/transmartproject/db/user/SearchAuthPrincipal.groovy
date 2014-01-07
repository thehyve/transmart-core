package org.transmartproject.db.user

class SearchAuthPrincipal {

    String  principalType
    String  description
    String  name
    String  uniqueId
    Boolean enabled
    Date    dateCreated
    Date    lastUpdated

    static mapping = {
        table   schema:    'searchapp'
		id      generator: 'assigned'

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
