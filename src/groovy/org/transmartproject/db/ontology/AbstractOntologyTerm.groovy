package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes

abstract class AbstractOntologyTerm implements OntologyTerm {

    String cVisualattributes = ''

    @Override
    EnumSet<VisualAttributes> getVisualAttributes() {
        def result = EnumSet.noneOf(VisualAttributes);

        cVisualattributes.each {
            def attribute = VisualAttributes.forKeyChar(it as Character);
            if (attribute) {
                result.add(attribute)
            }
        }

        result
    }

    @Override
    List<OntologyTerm> getChildren() {
        I2b2.withCriteria {
            eq      'cPath', fullName
            order   name, 'asc'
        }
    }
}
