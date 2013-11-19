package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.biomarker.BioMarker

@EqualsAndHashCode(includes = [ 'gplId', 'probesetId' ])
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

    BioMarker getBioMarkerGene() {
        BioMarker.findByPrimaryExternalId(geneId as String)
    }
}
