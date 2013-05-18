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
     * Can be null
     *
     * @return the trial-specific id or null
     */
    String getInTrialId()

    /**
     * The assays/samples used to generate a row of high-dimensional data.
     *
     * @return the assays associated with this patient; an empty list if none
     */
    Set<Assay> getAssays()

}
