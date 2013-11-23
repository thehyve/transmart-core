package org.transmartproject.db.dataquery.highdim

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.acgh.AcghValues
import org.transmartproject.core.dataquery.highdim.acgh.CopyNumberState
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = [ 'assay', 'region' ])
class DeSubjectAcghData implements AcghValues, Serializable {

    String trialName
    Double chipCopyNumberValue
    Double segmentCopyNumberValue
    Short  flag
    Double probabilityOfLoss
    Double probabilityOfNormal
    Double probabilityOfGain
    Double probabilityOfAmplification

    // see comment in mapping
    DeChromosomalRegion jRegion

    /* unused; should be the same as assay.patient */
    PatientDimension patient

    static belongsTo = [region: DeChromosomalRegion,
                        assay:  DeSubjectSampleMapping]

    static transients = ['copyNumberState']

	static mapping = {
        table   schema:    'deapp'

		id      composite: [ 'assay', 'region' ]

        chipCopyNumberValue        column: 'chip'
        segmentCopyNumberValue     column: 'segmented'
        probabilityOfLoss          column: 'probloss'
        probabilityOfNormal        column: 'probnorm'
        probabilityOfGain          column: 'probgain'
        probabilityOfAmplification column: 'probamp'

        /* references */
        region   column: 'region_id'
        assay    column: 'assay_id'
        patient  column: 'patient_id'

        // this duplicate mapping is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jRegion  column: 'region_id', insertable: false, updateable: false

        sort    assay:  'asc'

		version false
	}

	static constraints = {
        trialName                  nullable: true, maxSize: 50
        chipCopyNumberValue        nullable: true, scale:   17
        segmentCopyNumberValue     nullable: true, scale:   17
        flag                       nullable: true
        probabilityOfLoss          nullable: true, scale:   17
        probabilityOfNormal        nullable: true, scale:   17
        probabilityOfGain          nullable: true, scale:   17
        probabilityOfAmplification nullable: true, scale:   17
        patient                    nullable: true
	}

    @Override
    CopyNumberState getCopyNumberState() {
        CopyNumberState.forInteger(flag.intValue())
    }
}
