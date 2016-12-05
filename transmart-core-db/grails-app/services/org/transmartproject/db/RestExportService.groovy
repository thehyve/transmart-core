package org.transmartproject.db

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.export.Tasks.DataFetchTask
import org.transmartproject.export.Tasks.DataFetchTaskFactory
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.export.Tasks.DataExportFetchTask
import org.transmartproject.export.Tasks.DataExportFetchTaskFactory
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.ontology.ConceptsResource

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL

@Transactional
class RestExportService {

    @Autowired
    DataExportFetchTaskFactory dataExportFetchTaskFactory

    List<File> export(arguments) {
        DataExportFetchTask task = dataExportFetchTaskFactory.createTask(arguments)
        task.getTsv()
    }
}
