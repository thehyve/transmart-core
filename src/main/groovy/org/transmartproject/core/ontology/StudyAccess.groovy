package org.transmartproject.core.ontology

/**
 * StudyAccess is used for creating a bean where study and access to this study are defined.
 */
interface StudyAccess {

    /**
     * The study for which is determined if the user has access to the study or not
     * @return the study object
     */
    Study getStudy()

    /**
     * A map with different types of accessibility, used for letting the front-end know
     * which kind of access the user has.
     * @return boolean to determine if the user has access to the study or not.
     */
    Map getAccessibility()

}
