package org.transmartproject.core.dataquery

import org.transmartproject.core.dataquery.assay.Assay

/**
 * A patient (or subject) is a individual for which we have clinical
 * observations or other data.
 */
public interface Patient {

    /**
     * A unique identifier for the patient. Cannot be null.
     *
     * @return object's numeric identifier
     */
    Long getId()

    /**
     * The trial/study/experiment this patient belongs to.
     *
     * @return the trial name (all uppercase) or null
     */
    String getTrial()

    /**
     * The trial/study/experiment specific identifier for this patient. This
     * is generally something vaguely more user-readable than the patient id.
     * Can be null.
     *
     * @return the trial-specific id or null
     */
    String getInTrialId()

    /**
     * The sex of the subject. Cannot be null. If the sex is not known,
     * {@link Sex#UNKNOWN} is returned.
     *
     * @return sex of the subject
     */
    Sex getSex()

    /**
     * The birth data of the patient, or null if not available.
     *
     * @return patient birth date or null
     */
    Date getBirthDate()

    /**
     * The date of the death of the patient, or null if not available.
     *
     * @return patient death date or null
     */
    Date getDeathDate()

    /**
     * The age of the patient during the trial, or null if not available.
     *
     * @return age of the patient or null
     */
    Long getAge()

    /**
     * String describing the race of the patient, or null if not available.
     *
     * @return race of the patient or null
     */
    String getRace()

    /**
     * String describing the marital status of the patient (e.g. single, married,
     * divorced), or null if unavailable.
     *
     * @return marital status of the patient or null
     */
    String getMaritalStatus()

    /**
     * String describing the religion of the patient, or null if not available.
     *
     * @return the religion of the patient or null
     */
    String getReligion()

    /**
     * The assays/samples used to generate a row of high-dimensional data.
     *
     * @return the assays associated with this patient; an empty list if none
     */
    Set<Assay> getAssays()

}
