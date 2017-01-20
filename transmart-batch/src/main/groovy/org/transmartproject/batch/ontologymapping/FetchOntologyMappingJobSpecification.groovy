package org.transmartproject.batch.ontologymapping

import com.google.common.collect.ImmutableSet
import groovy.transform.TypeChecked
import org.transmartproject.batch.clinical.ClinicalJobSpecification
import org.transmartproject.batch.startup.*

/**
 * Processes the ontologymapping.params files.
 */
@TypeChecked
final class FetchOntologyMappingJobSpecification
        implements ExternalJobParametersModule, JobSpecification {

    public final static String ONTOLOGY_SERVICE_TYPE = 'ONTOLOGY_SERVICE_TYPE'
    public final static String ONTOLOGY_SERVER_URL = 'ONTOLOGY_SERVER_URL'
    public final static String ONTOLOGY_SERVER_SEARCH_PATH = 'ONTOLOGY_SERVER_SEARCH_PATH'
    public final static String ONTOLOGY_SERVER_DETAILS_PATH = 'ONTOLOGY_SERVER_DETAILS_PATH'
    public final static String ONTOLOGY_SERVER_API_KEY = 'ONTOLOGY_SERVER_API_KEY'
    public final static String ONTOLOGY_SERVER_ONTOLOGIES = 'ONTOLOGY_SERVER_ONTOLOGIES'

    public final static String ONTOLOGY_MAP_FILE = 'ONTOLOGY_MAP_FILE'
    public final static String COLUMN_MAP_FILE = ClinicalJobSpecification.COLUMN_MAP_FILE

    final List<? extends ExternalJobParametersModule> jobParametersModules = [
            new StudyJobParametersModule(),
            this]

    final Class jobPath = FetchOntologyMappingJobConfiguration

    final Set<String> supportedParameters = ImmutableSet.of(
            ONTOLOGY_SERVICE_TYPE,
            ONTOLOGY_SERVER_URL,
            ONTOLOGY_SERVER_SEARCH_PATH,
            ONTOLOGY_SERVER_DETAILS_PATH,
            ONTOLOGY_SERVER_API_KEY,
            ONTOLOGY_SERVER_ONTOLOGIES,
            ONTOLOGY_MAP_FILE,
            COLUMN_MAP_FILE
    )

    void munge(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (ejp[ONTOLOGY_MAP_FILE]) {
            ejp[ONTOLOGY_MAP_FILE] = convertRelativeWritePath ejp, ONTOLOGY_MAP_FILE
        }
        if (ejp[COLUMN_MAP_FILE]) {
            ejp[COLUMN_MAP_FILE] = convertRelativePath ejp, COLUMN_MAP_FILE
        }
    }
}
