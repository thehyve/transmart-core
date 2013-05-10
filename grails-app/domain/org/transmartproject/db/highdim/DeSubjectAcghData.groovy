package org.transmartproject.db.highdim

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.transmartproject.core.dataquery.acgh.ACGHValues
import org.transmartproject.core.dataquery.acgh.CopyNumberState
import org.transmartproject.db.i2b2data.PatientDimension

class DeSubjectAcghData implements ACGHValues, Serializable {

    String trialName
    Double chipCopyNumberValue
    Double segmentCopyNumberValue
    Short  flag
    Double probabilityOfLoss
    Double probabilityOfNormal
    Double probabilityOfGain
    Double probabilityOfAmplification

    /* unused; should be the same as assay.patient */
    PatientDimension patient

    /* unused */
    static belongsTo = [region:  DeChromosomalRegion,
                        assay:   DeSubjectSampleMapping]

    static transients = ['copyNumberState']

	static mapping = {
        table   schema:    'deapp'

		id      composite: ["assay", "region"]

        chipCopyNumberValue        column: 'chip'
        segmentCopyNumberValue     column: 'segmented'
        probabilityOfLoss          column: 'probloss'
        probabilityOfNormal        column: 'probnorm'
        probabilityOfGain          column: 'probgain'
        probabilityOfAmplification column: 'probamp'

        /* references */
        region  column: 'region_id'
        assay   column: 'assay_id'
        patient column: 'patient_id'

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

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append assay
        builder.append region
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append assay, other.assay
        builder.append region, other.region
        builder.isEquals()
    }

    @Override
    CopyNumberState getCopyNumberState() {
        CopyNumberState.forInteger(flag.intValue())
    }
}
