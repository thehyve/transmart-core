package org.transmartproject.db.ontology

class I2b2Secure extends AbstractI2b2Metadata implements Serializable {

    String secureObjectToken

    static String backingTable = 'I2B2_SECURE'

    static mapping = {
        table         name: 'I2B2_SECURE', schema: 'I2B2METADATA'
        version       false

        id composite: ['fullName', 'name']

        secureObjectToken column: 'secure_obj_token'

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
    }

    static constraints = {
        cSynonymCd          nullable:   false

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
    }

}
