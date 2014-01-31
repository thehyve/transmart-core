package jobs.table

import au.com.bytecode.opencsv.CSVWriter
import jobs.steps.OpenHighDimensionalDataStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm

/**
 * Created by carlos on 1/27/14.
 */
@Component
@Scope('job')
class ConceptTimeValuesTable {

    @Autowired
    ConceptsResource conceptsResource

    List<String> conceptPaths

    Closure<Boolean> enabledClosure

    @Lazy Map<String,Map> resultMap = computeMap()

    /**
     * @return map of concept_fullname -> series_meta map, or null if not enabled
     * or no common unit was found for all the concepts
     */
    private Map<String,Map> computeMap() {

        if (enabledClosure && !enabledClosure.call()) {
            return null
        }

        //get all the OntologyTerms for the concepts
        Set<OntologyTerm> terms = conceptPaths.collect {
            conceptsResource.getByKey(OpenHighDimensionalDataStep.createConceptKeyFrom(it))} as Set

        //get all the SeriesMeta mapped by concept name
        Map<String, Map> nameToSeriesMeta = terms.collectEntries {[it.fullName, it.metadata?.seriesMeta as Map]}
        int totalCount = nameToSeriesMeta.size()

        if (totalCount > 0) {
            String firstUnit = nameToSeriesMeta.values().first()?.unit?.toString()

            //if all the units are the same and not null, and with numerical values
            if (firstUnit != null &&
                    nameToSeriesMeta.values().count { firstUnit == it?.unit?.toString()} == totalCount &&
                    nameToSeriesMeta.values().count { it?.value?.isInteger() } == totalCount) {

                return nameToSeriesMeta
            }
        }

        return null //nothing to return
    }

}
