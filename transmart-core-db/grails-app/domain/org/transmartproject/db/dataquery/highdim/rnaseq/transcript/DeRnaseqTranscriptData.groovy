/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping


@EqualsAndHashCode(includes = ['assay', 'transcript'])
class DeRnaseqTranscriptData implements RnaSeqValues, Serializable {

    Integer readcount
    Double normalizedReadcount
    Double logNormalizedReadcount
    Double zscore

    static belongsTo = [
            assay     : DeSubjectSampleMapping,
            transcript: DeRnaseqTranscriptAnnot
    ]


    static mapping = {
        table schema: 'deapp'

        id composite: ['assay', 'transcript']

        version false
    }

    static constraints = {
        readcount nullable: true
        normalizedReadcount nullable: true
        logNormalizedReadcount nullable: true
        zscore nullable: true


    }
}
