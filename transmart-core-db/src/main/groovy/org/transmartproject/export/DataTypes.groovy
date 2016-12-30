package org.transmartproject.export

import org.transmartproject.core.ontology.OntologyTerm

class Datatypes {

    /*
    ontologyTermsMap has a resultinstance ID (also known as cohort ID) as key,
     with the concepts in the corresponding cohort as value.
     */
    Map<Integer, List<OntologyTerm>> ontologyTermsMap = [:]

    String dataType

    String dataTypeCode

}
