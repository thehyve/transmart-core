package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = [ 'assayId', 'probesetId' ])
class DeSubjectMicroarrayDataCoreDb implements Serializable {

    String     trialName
    BigDecimal rawIntensity
    BigDecimal logIntensity /* log2(rawIntensity) */
    BigDecimal zscore

    /* not mapped (only used in Oracle?) */
    //String     trialSource

    /* not mapped (not used in practice) */
    //Long       sampleId
    //String     subjectId
    //BigDecimal newRaw
    //BigDecimal newLog
    //BigDecimal newZscore

    DeMrnaAnnotationCoreDb jProbe //see comment on mapping

    static belongsTo = [
            probe: DeMrnaAnnotationCoreDb,
            assay: DeSubjectSampleMapping,
            patient: PatientDimension,
    ]

	static mapping = {
        table    schema: 'deapp', name: 'de_subject_microarray_data'

		id       composite: [ 'assay', 'probe' ]

        probe    column: 'probeset_id'
        assay    column: 'assay_id'
        patient  column: 'patient_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jProbe   column: 'probeset_id', insertable: false, updateable: false

		version  false
	}

	static constraints = {
        trialName    nullable: true, maxSize: 50
        probe        nullable: true
        assay        nullable: true
        patient      nullable: true
        rawIntensity nullable: true
        logIntensity nullable: true
        zscore       nullable: true
        //trialSource  nullable: true, maxSize: 200
        //sampleId     nullable: true
        //subjectId    nullable: true, maxSize: 50
        //newRaw       nullable: true, scale:   4
        //newLog       nullable: true, scale:   4
        //newZscore    nullable: true, scale:   4
	}
}
