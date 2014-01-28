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

    private static final String[] HEADER = [ "GROUP", "VALUE" ]

    @Autowired
    ConceptsResource conceptsResource

    List<String> conceptPaths

    File outputFile

    void compute() {

        //makes sure the file is not there
        outputFile.delete()

        //get all the OntologyTerms for the concepts
        Set<OntologyTerm> terms = conceptPaths.collect { conceptsResource.getByKey(OpenHighDimensionalDataStep.createConceptKeyFrom(it))} as Set

        //get all the SeriesMeta mapped by concept name
        Map<String, Map> nameToSeriesMeta = terms.collectEntries {[it.fullName, it.metadata?.seriesMeta as Map]}

        if (nameToSeriesMeta.size() > 0) {
            String firstUnit = nameToSeriesMeta.values().first()?.unit?.toString()

            //if all the units are the same and not null
            if (firstUnit != null &&
                    nameToSeriesMeta.values().count {firstUnit == it?.unit?.toString()} == nameToSeriesMeta.size()) {

                //create the file
                writeFile(nameToSeriesMeta)
            }
        }

    }

    private String normalize(String path) {
        path.substring(0,1)
    }

    private void writeFile(Map<String, Map> map) {
        println map

        outputFile.withWriter { writer ->
            CSVWriter csvWriter = new CSVWriter(writer, '\t' as char)
            csvWriter.writeNext(HEADER)

            map.each {
                def line = [it.key, it.value.value] as String[]
                csvWriter.writeNext(line)
            }

        }
    }

}
