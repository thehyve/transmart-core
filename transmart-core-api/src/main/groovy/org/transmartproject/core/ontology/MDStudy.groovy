/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.ontology

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.users.ProtectedResource

/**
 * This class should ideally be called just 'Study', but that name is already taken by the tabular study.
 *
 */
interface MDStudy extends ProtectedResource {

    Long getId()

    String getName()

    Collection<Dimension> getDimensions()

    Dimension getDimensionByName(String name)

    //Todo: also expose the org.transmart.biomart.Experiment?
    Long getBioExperimentId()

    StudyMetadata getMetadata()
}
