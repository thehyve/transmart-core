package org.transmartproject.db.highdim

import org.transmartproject.core.dataquery.acgh.Region

class DeChromosomalRegion implements Region {

    String  chromosome
    Long    start
    Long    end
    Integer numberOfProbes
    String  name
    String  cytoband

    /* unused */
    String  geneSymbol
    Long    geneId
    String  organism

	static hasMany = [aCGHValues: DeSubjectAcghData]

    static belongsTo = [platform: DeGplInfo]

	static mapping = {
        table          schema: 'deapp'

        id             column:  "region_id",  generator: "assigned"

        start          column: 'start_bp'
        end            column: 'end_bp'
        name           column: 'region_name'
        numberOfProbes column: 'num_probes'
        platform       column: 'gpl_id'

        version false

	}

	static constraints = {
        platform       nullable: true
        chromosome     nullable: true, maxSize: 2
        start          nullable: true
        end            nullable: true
        numberOfProbes nullable: true
        name           nullable: true, maxSize: 100
        cytoband       nullable: true, maxSize: 100
        geneSymbol     nullable: true, maxSize: 100
        geneId         nullable: true
        organism       nullable: true, maxSize: 200
	}

}
