package org.transmartproject.db.dataquery.highdim.rnaseqcog

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeRnaseqAnnotation implements Serializable {

    String id
    String geneSymbol
    String geneId /* the Entrez accession; "primary external id" */

    // irrelevant
    //String organism

    static belongsTo = [ platform: DeGplInfo ]

    static mapping = {
        table    schema: 'deapp'
        id       column: 'transcript_id', generator: 'assigned'

        platform column: 'gpl_id'
        version  false
    }

    static constraints = {
        geneSymbol   nullable: true, maxSize: 50
        geneId       nullable: true, maxSize: 50

        //organism nullable: true, maxSize: 30
    }
}
