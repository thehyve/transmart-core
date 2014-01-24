package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeMetaboliteSuperPathway {

	DeGplInfo gplId
	String name

	static hasMany = [subPathways: DeMetaboliteSubPathway]

	static mapping = {
        table schema: 'deapp', name: 'de_metabolite_super_pathways'
		id generator: 'assigned'
        name column: 'super_pathway_name'
		version false
	}

	static constraints = {
		name nullable: true, maxSize: 200
	}
}
