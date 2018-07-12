package org.transmartproject.core.multidimquery

import org.transmartproject.core.ontology.MDStudy

/**
 * The trial visit represents relative time within a study.
 */
interface TrialVisit {

    /**
     * Time unit, e.g., 'week'
     */
    String getRelTimeUnit()

    /**
     * Relative time value
     */
    Integer getRelTime()

    /**
     * Label for the trial visit, e.g., 'baseline' or 'first week'
     */
    String getRelTimeLabel()

    /**
     * The study the trial visit belongs to.
     */
    MDStudy getStudy()

}
