package org.transmartproject.db.dataquery.highdim.rbm

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = ['assay', 'annotation'])
class DeSubjectRbmData implements Serializable {

    BigDecimal       value
    BigDecimal       zscore

    // irrelevant
    //String           trialName
    //String           antigenName
    //String           geneSymbol
    //Long             geneId
    //String           conceptCd
    //String           timepoint
    //String           dataUid

    //BigDecimal       NValue
    //BigDecimal       normalizedValue
    //BigDecimal       logIntensity
    //BigDecimal       meanIntensity
    //BigDecimal       stddevIntensity
    //BigDecimal       medianIntensity
    //String           unit

    //String           rbmPanel

    //PatientDimension patient

    DeRbmAnnotation  deRbmAnnotation

    static belongsTo = [
            annotation: DeRbmAnnotation,
            assay: DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp', name: 'de_subject_rbm_data'

        id composite: ['assay', 'annotation']

        annotation column: 'rbm_annotation_id'
        assay      column: 'assay_id'

        // irrelevant
        //patient    column: 'patient_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        deRbmAnnotation column: 'rbm_annotation_id', insertable: false, updateable: false

        version false
    }

    static constraints = {
        annotation      nullable: true
        assay           nullable: true
        value           nullable: true, scale: 17
        zscore          nullable: true, scale: 17

        // irrelevant
        //trialName       nullable: true, maxSize: 100
        //antigenName     nullable: true, maxSize: 100
        //'NValue'        nullable: true
        //geneSymbol      nullable: true, maxSize: 100
        //geneId          nullable: true
        //normalizedValue nullable: true, scale:   5
        //conceptCd       nullable: true, maxSize: 200
        //timepoint       nullable: true, maxSize: 100
        //dataUid         nullable: true, maxSize: 100
        //logIntensity    nullable: true
        //meanIntensity   nullable: true
        //stddevIntensity nullable: true
        //medianIntensity nullable: true
        //rbmPanel        nullable: true, maxSize: 50
        //unit            nullable: true, maxSize: 200
    }
}
