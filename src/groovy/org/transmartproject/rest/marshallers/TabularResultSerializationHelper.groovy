package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

class TabularResultSerializationHelper implements HalOrJsonSerializationHelper<TabularResult<TerminalConceptVariable, PatientRow>> {

    final Class targetType = ObservationFact

    final String collectionName = 'observations'

    @Override
    Collection<Link> getLinks(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        [] as Collection
    }

    @Override
    Map<String, Object> convertToMap(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        [conceptCode: observationFact.conceptCode,
                valueType: observationFact.valueType,
                numberValue: observationFact.numberValue,
                textValue: observationFact.textValue,
                valueFlag: observationFact.valueFlag,
                sourcesystemCd: observationFact.sourcesystemCd]
    }

    @Override
    Set<String> getEmbeddedEntities(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        [] as Set
    }

}
