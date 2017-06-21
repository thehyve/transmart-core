/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.i2b2data

import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.storage.LinkedFileCollection

/**
 * Domain class that represents the link between a study and observation data
 * in Transmart.
 *
 * Every observation is associated with a {@link TrialVisit}, which is associated with
 * a study.
 * Access to observation data is controlled using the {@link Study#secureObjectToken} field.
 * If the field contains the value {@link Study#PUBLIC}, the study is considered to be public.
 * Otherwise, access should be checked using {@link org.transmartproject.db.accesscontrol.SecuredObject},
 * where the value of {@link Study#secureObjectToken} should be equal to the value of
 * {@link org.transmartproject.db.accesscontrol.SecuredObject#bioDataUniqueId}.
 *
 * Metadata is available in the form of a label {@link Study#studyId} and a link to the <code>bio_experiment</code>
 * table in {@link Study#bioExperimentId}.
 */
class Study implements MDStudy {

    static final String PUBLIC = 'PUBLIC'

    /**
     * String label (optional)
     * E.g., GSE8581.
     */
    String studyId

    /**
     * Refers to {@link org.transmartproject.db.accesscontrol.SecuredObject#bioDataUniqueId}.
     */
    String secureObjectToken

    /**
     * Refers to {@link org.transmart.biomart.Experiment#id}.
     */
    Long bioExperimentId

    static constraints = {
        studyId             maxSize: 100
        secureObjectToken   maxSize: 200
        bioExperimentId     nullable: true
    }

    static hasMany = [
        trialVisits: TrialVisit,
        dimensionDescriptions: DimensionDescription,
    ]

    static mapping = {
        table               name: 'study', schema: 'i2b2demodata'
        id                  column: 'study_num', type: Long, generator: 'sequence', params: [sequence: 'study_num_seq']
        studyId             column: 'study_id'
        secureObjectToken   column: 'secure_obj_token'
        bioExperimentId     column: 'bio_experiment_id'
        dimensionDescriptions joinTable: [schema: 'i2b2metadata']
        version false
    }

    @Override String getName() {
        studyId
    }

    @Override List<DimensionImpl> getDimensions() {
        dimensionDescriptions*.dimension
    }

    @Override DimensionImpl getDimensionByName(String name) {
        def dd = dimensionDescriptions.find {it.name == name}
        if(dd == null) throw new NoSuchElementException("This study does not have a $name dimension")
        dd.dimension
    }

}
