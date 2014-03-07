package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

class TabularResultSerializationHelper implements HalOrJsonSerializationHelper<TabularResult<TerminalConceptVariable, PatientRow>> {

    final Class targetType = TabularResult

    final String collectionName = 'observations'

    @Override
    Collection<Link> getLinks(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        [] as Collection
    }

    @Override
    Map<String, Object> convertToMap(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        def observations = []

        def concepts = tabularResult.getIndicesList()
        tabularResult.getRows().each { row ->
            concepts.each {concept ->
                def value = row.getAt(concept)
                observations << new ObservationWrapper(
                        subject: row.patient,
                        concept: concept,
                        value: value
                )
            }
        }
        [observations: observations]
    }

    @Override
    Set<String> getEmbeddedEntities(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        [] as Set
    }

}
