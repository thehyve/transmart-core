/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeRnaseqTranscriptAnnot {

    String chromosome
    Long start
    Long end
    String refId
    String transcript

    static belongsTo = [platform: DeGplInfo]

    static mapping = {
        table schema: 'deapp'

        id generator: "assigned"
        start column: 'start_bp'
        end column: 'end_bp'
        platform column: 'gpl_id'

        version false
    }

    static constraints = {
        chromosome nullable: false, maxSize: 2
        start nullable: false
        end nullable: false
        platform nullable: false
        refId nullable: false
        transcript nullable: true

    }
}
