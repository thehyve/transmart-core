package org.transmartproject.db.highdim

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.transmartproject.core.dataquery.rnaseq.RNASEQValues
import org.transmartproject.db.i2b2data.PatientDimension

class DeSubjectRnaseqData implements RNASEQValues, Serializable {

    String trialName
    Integer readCountValue

    /* unused; should be the same as assay.patient */
    PatientDimension patient

    /* unused */
    static belongsTo = [region:  DeChromosomalRegion,
                        assay:   DeSubjectSampleMapping]

    static mapping = {
       	table   schema:    'deapp'

	id      composite: ["assay", "region"]

	readCountValue        column: 'readcount'

	/* references */
	region  column: 'region_id'
	assay   column: 'assay_id'
	patient column: 'patient_id'
	sort    assay:  'asc'

	version false
    }

    static constraints = {
        trialName                  nullable: true, maxSize: 50
        readCountValue             nullable: true
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
}
