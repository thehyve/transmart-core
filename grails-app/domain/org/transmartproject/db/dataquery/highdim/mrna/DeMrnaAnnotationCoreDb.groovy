package org.transmartproject.db.dataquery.highdim.mrna

import org.transmartproject.db.biomarker.BioMarkerCoreDb

class DeMrnaAnnotationCoreDb {

    String     gplId
    String     probeId    /* a string user probe name */
    String     geneSymbol
    Long       geneId     /* aka primary external id */
    String     organism

    static transients = [ 'bioMarkerGene' ]

    static mapping = {
        id      column: 'probeset_id',       generator: 'assigned'
        table   name:  'de_mrna_annotation', schema:     'deapp'

        sort    id: 'asc'

        version false
    }

    static constraints = {
        gplId      nullable: true, maxSize: 100
        probeId    nullable: true, maxSize: 100
        geneSymbol nullable: true, maxSize: 100
        geneId     nullable: true
        organism   nullable: true, maxSize: 200
    }

    BioMarkerCoreDb getBioMarkerGene() {
        BioMarkerCoreDb.findByPrimaryExternalId(geneId as String)
    }
}
