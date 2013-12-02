package org.transmartproject.db.dataquery.highdim.protein

class DeProteinAnnotation {

    String gplId
    String peptide
    String uniprotId
    String biomarkerId
    String organism

    static hasMany = [ dataRows: DeSubjectProteinData ]

    static mappedBy = [ dataRows: 'annotation' ]

    static mapping = {
        table   schema:    'deapp'
        id      generator: 'assigned'
        version false
    }

    static constraints = {
        gplId       maxSize:  50
        peptide     maxSize:  800
        uniprotId   nullable: true, maxSize: 200
        biomarkerId nullable: true, maxSize: 400
        organism    nullable: true, maxSize: 800
    }
}
