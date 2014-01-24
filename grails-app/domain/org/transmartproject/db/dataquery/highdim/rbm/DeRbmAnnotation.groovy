package org.transmartproject.db.dataquery.highdim.rbm

class DeRbmAnnotation {

    String gplId
    String antigenName
    String uniprotId
    String geneSymbol
    String geneId

    static mapping = {
        table   schema:    'deapp',   name: 'de_rbm_annotation'
        id      generator: 'assigned'
        version false
    }

    static constraints = {
        gplId       maxSize:  50
        antigenName maxSize:  800
        uniprotId   nullable: true, maxSize: 200
        geneSymbol  nullable: true, maxSize: 200
        geneId      nullable: true, maxSize: 400
    }
}
