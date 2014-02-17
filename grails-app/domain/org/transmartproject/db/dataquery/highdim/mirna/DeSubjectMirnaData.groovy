package org.transmartproject.db.dataquery.highdim.mirna

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

@EqualsAndHashCode(includes = 'assay,probe')
class DeSubjectMirnaData implements Serializable {

    BigDecimal rawIntensity
    BigDecimal logIntensity
    BigDecimal zscore

    DeQpcrMirnaAnnotation jProbe //see comment on mapping

    // irrelevant
    //String trialSource
    //String trialName
    //PatientDimension patient

    static belongsTo = [
            assay: DeSubjectSampleMapping,
            probe: DeQpcrMirnaAnnotation,
    ]


    static mapping = {
        table schema: 'deapp'
        id    composite: ['assay', 'probe']

        assay   column: 'assay_id'
        probe   column: 'probeset_id'

        // irrelevant
        //patient column: 'patient_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jProbe column: 'probeset_id', insertable: false, updateable: false

        version false
    }

    static constraints = {
        assay        nullable: true
        rawIntensity nullable: true, scale: 4
        logIntensity nullable: true, scale: 4
        zscore       nullable: true, scale: 4
        //trialSource  nullable: true, maxSize: 200
        //trialName    nullable: true, maxSize: 50
        //patient      nullable: true
    }
}
