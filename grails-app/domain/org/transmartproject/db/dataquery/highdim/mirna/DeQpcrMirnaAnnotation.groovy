package org.transmartproject.db.dataquery.highdim.mirna

class DeQpcrMirnaAnnotation implements Serializable {

	String mirnaId
    String detector

    // unused or irrelevant:
    //String idRef
    //String probeId

    //String organism

	static mapping = {
        table    schema: 'deapp'
        id       column: 'probeset_id', generator: 'assigned'
        detector column: 'mirna_symbol' // column name is scheduled to be changed
        version  false
	}

	static constraints = {
        mirnaId     nullable: true, maxSize: 100
        detector    nullable: true, maxSize: 100

        // unused or irrelevant:
        //idRef       nullable: true, maxSize: 100
        //probeId     nullable: true, maxSize: 100
        //organism    nullable: true, maxSize: 2000
	}
}
