package org.transmartproject.batch.ontologymapping

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.clinical.ClinicalJobContext
import org.transmartproject.batch.clinical.ontology.OntologyNode
import org.transmartproject.batch.clinical.variable.ClinicalVariable

@Slf4j
class OntologyMappingGenerator implements ItemReader<OntologyNode> {

    @Autowired
    ClinicalJobContext ctx

    List<OntologyNode> nodes

    int index = 0

    def ontologyMap = [
            (['Vital Signs', 'Weight_KG']): [
                    code:       'SNOMEDCT/425024002',
                    label:      'Body weight without shoes',
                    uri:        'http://purl.bioontology.org/ontology/SNOMEDCT/425024002',
                    ancestors:  ['SNOMEDCT/27113001']],
            (['Clinical Chemistry', 'HDL mg/dl']): [
                    code:       'SNOMEDCT/17888004',
                    label:      'High density lipoprotein measurement',
                    uri:        'http://purl.bioontology.org/ontology/SNOMEDCT/17888004',
                    ancestors:  ['SNOMEDCT/104789001']]
    ]

    private void fetchOntologyNodes() {
        log.info "Fetching ontology nodes..."
        nodes = []
        ctx.variables.each { ClinicalVariable variable ->
            log.info "Fetching for variable ${variable.dataLabel}..."
            def mapping = ontologyMap[[variable.categoryCode, variable.dataLabel]]
            if (mapping) {
                log.info "Found mapping for ${variable.dataLabel}!"
                nodes << new OntologyNode(
                        categoryCode:   variable.categoryCode,
                        dataLabel:      variable.dataLabel,
                        code:           mapping.code,
                        label:          mapping.label,
                        uri:            mapping.uri,
                        ancestorCodes:  mapping.ancestors
                )
            }
        }
    }

    @Override
    OntologyNode read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!nodes) {
            fetchOntologyNodes()
        }
        if (index < nodes.size()) {
            def node = nodes[index]
            index++
            node
        } else {
            null
        }
    }
}
