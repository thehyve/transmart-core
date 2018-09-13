/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.ontology

import org.transmartproject.core.multidimquery.hypercube.Dimension

/**
 * This class should ideally be called just 'Study', but that name is already taken by the tabular study.
 *
 */
interface MDStudy {

    Long getId()

    String getName()

    String getSecureObjectToken()

    Collection<Dimension> getDimensions()

    Dimension getDimensionByName(String name)

    //Todo: also expose the org.transmart.biomart.Experiment?
    Long getBioExperimentId()

    StudyMetadata getMetadata()
}
