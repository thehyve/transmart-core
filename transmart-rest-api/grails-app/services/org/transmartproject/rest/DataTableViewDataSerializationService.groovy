package org.transmartproject.rest

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.multidimquery.datatable.PaginationParameters
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.AbstractDataResourceService
import org.transmartproject.rest.serialization.DataTableSerializer
import org.transmartproject.rest.serialization.tabular.DataTableTSVSerializer

import java.util.zip.ZipOutputStream

/**
 * Data serialisation service for the dataTable data view.
 * Only supports the table operations.
 */
@Slf4j
@Transactional
class DataTableViewDataSerializationService extends AbstractDataResourceService {

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    /**
     * Write tabular data to the output stream as TSV
     *
     * @param format
     * @param constraint
     * @param tableConfig
     * @param user
     * @param out
     */
    void writeTableToTsv(Constraint constraint,
                    TableConfig tableConfig,
                    User user,
                    ZipOutputStream out) {
        checkAccess(constraint, user, PatientDataAccessLevel.MEASUREMENTS)
        StreamingDataTable datatable = multiDimensionalDataResource.retrieveStreamingDataTable(
                tableConfig, 'clinical', constraint, user)
        try {
            log.info "Writing tabular data in TSV format."
            def serializer = new DataTableTSVSerializer(user, out)
            serializer.writeDataTableToZip(datatable)
        } finally {
            datatable.close()
            log.info "Writing tabular data in TSV format completed."
        }
    }

    /**
     * Write tabular data page to the output stream as JSON
     *
     * @param format
     * @param constraint
     * @param tableConfig
     * @param pagination
     * @param user
     * @param out
     */
    void writeTablePageToJson(Constraint constraint,
                        TableConfig tableConfig,
                        PaginationParameters pagination,
                        User user,
                        OutputStream out) {
        checkAccess(constraint, user, PatientDataAccessLevel.MEASUREMENTS)
        def datatable = multiDimensionalDataResource.retrieveDataTablePage(tableConfig, pagination, 'clinical', constraint, user)
        try {
            DataTableSerializer.write(datatable, out)
        } finally {
            datatable.close()
        }
    }

}
