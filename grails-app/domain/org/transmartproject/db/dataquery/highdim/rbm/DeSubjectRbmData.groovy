package org.transmartproject.db.dataquery.highdim.rbm

import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class DeSubjectRbmData {

    BigDecimal value
    BigDecimal logIntensity
    BigDecimal zscore
    String unit

    static hasMany = [annotations: DeRbmAnnotation]

    static belongsTo = [
            annotations: DeRbmAnnotation,
            assay: DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp', name: 'de_subject_rbm_data'
        id generator: 'sequence', params: [sequence: 'de_subject_rbm_data_seq', schema: 'deapp']
        assay column: 'assay_id'
        annotations joinTable: [
                name: 'de_rbm_data_annotation_join',
                column: 'annotation_id',
                key: 'data_id']
        version false
    }

    static constraints = {
        assay nullable: true
        value nullable: true, scale: 17
        zscore nullable: true, scale: 17
        logIntensity nullable: true, scale: 17
        unit nullable: true, maxSize: 150
    }
}
