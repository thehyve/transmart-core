package org.transmartproject.core.dataquery.assay

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.highdim.Platform

/**
 * An assay (used somewhat interchangeably with the term "sample" for tranSMART
 * purposes) is any biological tissue used in an analysis for generating
 * high-dimensional data. An assay is linked to a specific patient and to a
 * specific platform.
 */
interface Assay {

    /**
     * A numeric identifier for this assay.
     */
    Long getId()

    /**
     * The patient from whom a sample was extracted for this assay.
     *
     * @return the patient for this assay; never null
     */
    Patient getPatient()

    /**
     * Shortcut to <code>patient.inTrialId<code>.
     * The reason this method exists is that this external patient id is
     * generally used to link an assay to clinical data and for performance
     * reasons.
     *
     * @return the patient's "in trial id", or null if it doesn't exist
     */
    String getPatientInTrialId()

    /**
     * The trial for which this assay was done. An all-uppercase name.
     *
     * @return The all-uppercase trial name; never null
     */
    String getTrialName()

    /**
     * The timepoint at which this assay was conducted. May be null.
     *
     * @return the nullable assay timepoint
     */
    Timepoint getTimepoint()

    /**
     * A string representing the sample used in this assay.
     * @return string representing assay's sample name
     */
    String getSampleCode()

    /**
     * The sample type associated with this assay.
     *
     * @return the sample used in this assay or null if not available
     */
    SampleType getSampleType()

    /**
     * The tissue type associated with this assay.
     *
     * @return the tisue type, if any; null otherwise
     */
    TissueType getTissueType()

    /**
     * The platform for this assay. A platform defines metadata about the
     * high-dimensional data. It can, for instance, refer to the commercial
     * array used to analyse the sample.
     *
     * @return the platform for this assay; never null
     */
    Platform getPlatform()

}
