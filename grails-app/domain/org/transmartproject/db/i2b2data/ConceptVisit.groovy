package org.transmartproject.db.i2b2data

import org.transmartproject.db.i2b2data.ObservationFact

class ConceptVisit {

    String id
    String visitName

    static hasMany = [observations: ObservationFact]

    static mapping = {
        table name: 'DE_CONCEPT_VISIT', schema: 'DEAPP'
        version false

        id column: 'CONCEPT_CD', type: 'string'
        visitName column: 'VISIT_NAME',type:'string'
    }


}  