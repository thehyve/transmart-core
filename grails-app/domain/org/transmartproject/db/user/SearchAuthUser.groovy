package org.transmartproject.db.user

class SearchAuthUser {

	String  email
	Boolean emailShow
	String  hash
	String  userRealName
	String  username

    /* not mapped (only on thehyve/master) */
	//String federatedId

	static mapping = {
        table   schema:    'searchapp'
		id      generator: 'assigned'

        hash    column: 'passwd'

		version false
	}

	static constraints = {
        email        nullable: true
        emailShow    nullable: true
        hash         nullable: true
        userRealName nullable: true
        username     nullable: true
		//federatedId nullable: true, unique: true
	}
}
