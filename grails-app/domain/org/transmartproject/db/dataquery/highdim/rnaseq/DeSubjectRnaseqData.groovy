package org.transmartproject.db.dataquery.highdim.rnaseq

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = [ 'assay', 'region' ])
class DeSubjectRnaseqData implements RnaSeqValues, Serializable {

    String trialName
    Integer readCount

    // see comment in mapping
    DeChromosomalRegion jRegion

    /* unused; should be the same as assay.patient */
    PatientDimension patient

    static belongsTo = [
            region: DeChromosomalRegion,
            assay:  DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp'

        id composite: [ 'assay', 'region' ]


        /* references */
        region    column: 'region_id'
        assay     column: 'assay_id'
        patient   column: 'patient_id'
        readCount column: 'readcount'

        // this duplicate mapping is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jRegion  column: 'region_id', insertable: false, updateable: false

        sort    assay:  'asc'

        version     false
    }

    static constraints = {
        trialName    nullable: true, maxSize: 50
        region       nullable: true
        assay        nullable: true
        readCount    nullable: true
        patient      nullable: true
    }
}
