package org.transmartproject.db.dataquery.highdim.acgh

import org.transmartproject.core.dataquery.highdim.acgh.Region
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.acgh.DeSubjectAcghData

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

    static mappedBy = [ aCGHValues: 'region' ]

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
