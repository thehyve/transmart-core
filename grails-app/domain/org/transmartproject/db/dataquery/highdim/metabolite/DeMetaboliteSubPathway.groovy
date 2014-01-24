package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeMetaboliteSubPathway {

    DeGplInfo gplId
    String    name

    static hasMany = [annotations: DeMetaboliteAnnotation]

    static belongsTo = [superPathway: DeMetaboliteSuperPathway]

    static mapping = {
        table        schema:    'deapp',   name: 'de_metabolite_sub_pathways'
        id           generator: 'assigned'

        annotations  joinTable: [name:   'de_metabolite_sub_pway_metab',
                                 key:    'sub_pathway_id',
                                 column: 'metabolite_id']
        name         column: 'sub_pathway_name'
        superPathway column: 'super_pathway_id'

        version     false
    }

    static constraints = {
        name           maxSize:  200
        superPathway   nullable: true
    }
}
