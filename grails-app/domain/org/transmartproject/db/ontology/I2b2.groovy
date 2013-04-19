package org.transmartproject.db.ontology

class I2b2 extends AbstractI2b2Metadata implements Serializable {

    BigDecimal   cTotalnum
    String       cComment
    String       mAppliedPath
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    String       valuetypeCd
    String       mExclusionCd
    String       cPath
    String       cSymbol

    static String backingTable = 'I2B2'

    static mapping = {
        table         name: 'I2B2', schema: 'I2B2METADATA'
        version       false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id          composite: ['fullName', 'name']

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
	}

	static constraints = {
        cTotalnum           nullable:   true
        cComment            nullable:   true
        mAppliedPath        nullable:   false,   maxSize:   700
        downloadDate        nullable:   true
        updateDate          nullable:   false
        importDate          nullable:   true
        sourcesystemCd      nullable:   true,    maxSize:   50
        valuetypeCd         nullable:   true,    maxSize:   50
        mExclusionCd        nullable:   true,    maxSize:   25
        cPath               nullable:   true,    maxSize:   700
        cSymbol             nullable:   true,    maxSize:   50

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
	}

}
