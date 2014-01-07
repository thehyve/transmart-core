package org.transmartproject.db.i2b2data

class ConceptDimension {

    String       conceptPath
    String       conceptCode

    // not used
    //String       nameChar
    //String       conceptBlob
    //Date         updateDate
    //Date         downloadDate
    //Date         importDate
    //String       sourcesystemCd
    //BigDecimal   uploadId

    static mapping = {
        table   schema: 'i2b2demodata'
        id      name:   'conceptPath', generator: 'assigned'

        conceptCode column: 'concept_cd'

        version false
    }

    static constraints = {
        conceptPath      maxSize:    700
        conceptCode      maxSize:    50

        // not used
        //nameChar         nullable:   true,   maxSize:   2000
        //conceptBlob      nullable:   true
        //updateDate       nullable:   true
        //downloadDate     nullable:   true
        //importDate       nullable:   true
        //sourcesystemCd   nullable:   true,   maxSize:   50
        //uploadId         nullable:   true
    }
}
