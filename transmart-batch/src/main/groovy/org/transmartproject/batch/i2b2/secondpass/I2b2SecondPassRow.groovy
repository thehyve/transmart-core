package org.transmartproject.batch.i2b2.secondpass

import groovy.transform.ToString
import org.transmartproject.batch.i2b2.fact.FactGroup
import org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable
import org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable
import org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable

/**
 * The item type for the second pass.
 */
@ToString(includePackage = false, includeNames = true)
class I2b2SecondPassRow {

    Long patientNum
    Long encounterNum
    String providerId

    Date startDate
    Date endDate

    Map<PatientDimensionI2b2Variable, Object> patientDimensionValues
    Map<VisitDimensionI2b2Variable, Object> visitDimensionValues
    Map<ProviderDimensionI2b2Variable, Object> providerDimensionValues

    List<FactGroup> factGroups
}
