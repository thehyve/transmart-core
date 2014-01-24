package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeMetaboliteSubPathway {

	DeGplInfo gplId
	String name
	DeMetaboliteSuperPathway superPathway

	static hasMany = [annotations: DeMetaboliteAnnotation]

	static mapping = {
        table       schema: 'deapp', name: 'de_metabolite_sub_pathway'
		id          generator: 'assigned'

        annotations joinTable: [name: 'de_metabolite_sub_pway_metab', key: 'sub_pathway_id']
        name        column: 'sub_pathway_name'

        version     false
	}

	static constraints = {
		gplId          maxSize:  50
		name maxSize:  200
        superPathway   nullable: true
	}
}
