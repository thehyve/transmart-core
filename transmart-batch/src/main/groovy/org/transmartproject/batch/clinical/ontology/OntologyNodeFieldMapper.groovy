package org.transmartproject.batch.clinical.ontology

import groovy.transform.CompileStatic
import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Fill the fields of {@link org.transmartproject.batch.clinical.ontology.OntologyNode}.
 */
@Component
@JobScopeInterfaced
@CompileStatic
class OntologyNodeFieldMapper implements FieldSetMapper<OntologyNode> {

    @Override
    OntologyNode mapFieldSet(FieldSet fieldSet) throws BindException {
        def ancestorsString = fieldSet.readString('Ancestors').trim()
        def ancestorCodes = (ancestorsString.empty ? [] : ancestorsString.split(',')) as List<String>
        def node = new OntologyNode(
                categoryCode:   fieldSet.readString('Category code'),
                dataLabel:      fieldSet.readString('Data label'),
                code:           fieldSet.readString('Ontology code'),
                label:          fieldSet.readString('Label'),
                uri:            fieldSet.readString('URI'),
                ancestorCodes:  ancestorCodes
        )
        node
    }

}
