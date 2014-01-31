package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeProteinAnnotation {

    String   peptide
    String   uniprotId
    String   uniprotName

    // irrelevant
    //String biomarkerId
    //String organism

    static belongsTo = [ platform: DeGplInfo ]

    static hasMany = [ dataRows: DeSubjectProteinData ]

    static mappedBy = [ dataRows: 'annotation' ]

    static mapping = {
        table    schema:    'deapp'
        id       generator: 'assigned'
        platform column:    'gpl_id'

        version   false
    }

    static constraints = {
        peptide     maxSize:  800
        uniprotId   nullable: true, maxSize: 200
        uniprotName nullable: true, maxSize: 200

        //biomarkerId nullable: true, maxSize: 400
        //organism    nullable: true, maxSize: 800
    }
}
