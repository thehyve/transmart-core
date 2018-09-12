package org.transmartproject.rest

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.OperationNotImplementedException
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.datatable.PaginationParameters
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.multidimquery.export.Format
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User
import org.transmartproject.rest.serialization.*

/**
 * Data serialisation service for the dataTable data view.
 * Only supports the table operations.
 */
@Transactional
class DataTableViewDataSerializationService implements DataSerializer {

    @Autowired
    HypercubeDataSerializationService hypercubeDataSerializationService

    Set<Format> supportedFormats = [Format.TSV] as Set<Format>

    @Override
    void writeClinical(Format format, DataRetrievalParameters parameters, User user, OutputStream out) {
        throw new OperationNotImplementedException("Operation not implement for the data table view.")
    }

    @Override
    void writeTable(Format format, Constraint constraint, TableConfig tableConfig, User user, OutputStream out) {
        hypercubeDataSerializationService.writeTable(format, constraint, tableConfig, user, out)
    }

    @Override
    void writeTablePage(Format format, Constraint constraint, TableConfig tableConfig, PaginationParameters pagination, User user, OutputStream out) {
        hypercubeDataSerializationService.writeTablePage(format, constraint, tableConfig, pagination, user, out)
    }

    @Override
    void writeHighdim(Format format, String type, Constraint assayConstraint, Constraint biomarkerConstraint, String projection, User user, OutputStream out) {
        throw new OperationNotImplementedException("Operation not implement for the data table view.")
    }

}
