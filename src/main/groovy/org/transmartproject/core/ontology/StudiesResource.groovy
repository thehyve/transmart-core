package org.transmartproject.core.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException

/**
 * The studies resource represents the set of all the studies.
 */
interface StudiesResource {

    /**
     * Returns a set of all the studies
     * @return set of all studies
     */
    Set<Study> getStudySet()

    /**
     * Fetches a study by name. Study names are unique. This method is
     * case insensitive. Therefore, the value of the parameter <code>name</code>
     * may not match exactly the value of {@link Study#getName()}.
     *
     * @param name the name of the study
     * @return the study
     * @throws NoSuchResourceException if there's no study with such a name
     */
    Study getStudyByName(String name) throws NoSuchResourceException

    /**
     * Exchanges the top-level ontology term of a study with the corresponsing
     * study.
     *
     * @param term the top ontology term that corresponds to the desired study
     * @return the study
     * @throws NoSuchResourceException if the given ontology term doesn't
     * correspond to a top level node of a study
     */
    Study getStudyByOntologyTerm(OntologyTerm term) throws NoSuchResourceException

}
