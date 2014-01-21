package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeMetaboliteAnnotation {

    String biochemicalName
    String hmdbId

    static belongsTo = [ platform: DeGplInfo ]

    static hasMany = [ dataRows: DeSubjectMetabolomicsData ]

    static mappedBy = [ dataRows: 'annotation' ]

    static mapping = {
        table    schema:    'deapp'
        id       generator: 'assigned'
        platform column:    'gpl_id'

        version   false
    }
}
