package org.transmartproject.db.dataquery.highdim.rnaseqcog

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

@EqualsAndHashCode(includes = [ 'assay', 'annotation' ])
class DeSubjectRnaData implements Serializable {


    BigDecimal rawIntensity
    BigDecimal zscore

    DeRnaseqAnnotation jAnnotation //due to criteria bug

    // irrelevant
    //BigDecimal logIntensity
    //String     trialSource
    //String     trialName
    //Long       patientId

    static belongsTo = [
            annotation: DeRnaseqAnnotation,
            assay:      DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp'

        id composite: [ 'assay', 'annotation' ]

        annotation  column: 'probeset_id' // poor name; no probes involved
        assay       column: 'assay_id'

        // here due to criteria bug
        jAnnotation column: 'probeset_id', insertable: false, updateable: false

        version     false

    }

    static constraints = {
        annotation   nullable: true
        assay        nullable: true
        rawIntensity nullable: true, scale: 4
        zscore       nullable: true, scale: 4

        // irrelevant
        //trialSource  nullable: true, maxSize: 200
        //trialName    nullable: true, maxSize: 50
        //patientId    nullable: true
        //logIntensity nullable: true, scale: 4
    }
}
