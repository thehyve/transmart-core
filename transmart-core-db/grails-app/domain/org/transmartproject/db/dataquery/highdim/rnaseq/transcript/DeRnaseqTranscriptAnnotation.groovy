package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeRnaseqTranscriptAnnotation {

    String chromosome
    Long start
    Long end
    String transcriptId

    static belongsTo = [platform: DeGplInfo]

    static mapping = {
        table   schema: 'deapp'

        id generator: "assigned"

        version false
    }

    static constraints = {
        chromosome  nullable: false, maxSize: 2
        start       nullable: false
        end         nullable: false
        platform    nullable: false
        transcriptId nullable: true

    }
}
