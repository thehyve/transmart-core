package org.transmartproject.db.i2b2data

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * Currently only used to make grails create the table when running the
 * integration tests on a memory database.
 * All the details of this class are subject to change.
 */
class ObservationFact implements Serializable {

    BigDecimal   encounterNum
    String       conceptCd
    String       providerId
    Date         startDate
    String       modifierCd
    Long         instanceNum
    BigDecimal   patientNum
    String       valtypeCd //starting here they're nullable
    String       tvalChar
    BigDecimal   nvalNum
    String       valueflagCd
    BigDecimal   quantityNum
    String       unitsCd
    Date         endDate
    String       locationCd
    String       observationBlob
    BigDecimal   confidenceNum
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    BigDecimal   uploadId

	static mapping = {
        table   name: 'observation_fact', schema: 'I2B2DEMODATA'
		id      composite: ["encounterNum", "conceptCd", "providerId", "startDate", "modifierCd", "instanceNum"]
		version false
	}

	static constraints = {
        conceptCd         maxSize:    50
        providerId        maxSize:    50
        modifierCd        maxSize:    100
        valtypeCd         nullable:   true,   maxSize:   50
        tvalChar          nullable:   true
        nvalNum           nullable:   true,   scale:     5
        valueflagCd       nullable:   true,   maxSize:   50
        quantityNum       nullable:   true,   scale:     5
        unitsCd           nullable:   true,   maxSize:   50
        endDate           nullable:   true
        locationCd        nullable:   true,   maxSize:   50
        observationBlob   nullable:   true
        confidenceNum     nullable:   true,   scale:     5
        updateDate        nullable:   true
        downloadDate      nullable:   true
        importDate        nullable:   true
        sourcesystemCd    nullable:   true,   maxSize:   50
        uploadId          nullable:   true
	}

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append encounterNum
        builder.append conceptCd
        builder.append providerId
        builder.append startDate
        builder.append modifierCd
        builder.append instanceNum
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append encounterNum, other.encounterNum
        builder.append conceptCd,    other.conceptCd
        builder.append providerId,   other.providerId
        builder.append startDate,    other.startDate
        builder.append modifierCd,   other.modifierCd
        builder.append instanceNum,  other.instanceNum
        builder.isEquals()
    }
}
