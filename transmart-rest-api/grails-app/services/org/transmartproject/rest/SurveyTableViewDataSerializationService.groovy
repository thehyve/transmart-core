/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transmartproject.rest

import com.google.common.collect.ImmutableList
import grails.transaction.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.util.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.*
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.SurveyTableColumnService
import org.transmartproject.db.multidimquery.HypercubeDataColumn
import org.transmartproject.db.multidimquery.SurveyTableView
import org.transmartproject.db.support.ParallelPatientSetTaskService
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.rest.serialization.Format
import org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import java.util.zip.ZipOutputStream

import static org.transmartproject.db.support.ParallelPatientSetTaskService.SubtaskParameters
import static org.transmartproject.db.support.ParallelPatientSetTaskService.TaskParameters

@Slf4j
@Transactional
@CompileStatic
class SurveyTableViewDataSerializationService implements DataSerializer {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    SurveyTableColumnService surveyTableColumnService

    @Autowired
    ParallelPatientSetTaskService parallelPatientSetTaskService


    Set<Format> supportedFormats = [Format.TSV, Format.SPSS] as Set<Format>

    TabularResultSerializer getSerializer(Format format, User user,
                                          ZipOutputStream zipOutputStream,
                                          ImmutableList<DataColumn> columns,
                                          String fileName) {
        switch(format) {
            case Format.TSV:
                return new TabularResultTSVSerializer(user, zipOutputStream, columns)
            case Format.SPSS:
                return new TabularResultSPSSSerializer(user, zipOutputStream, columns, fileName)
            default:
                throw new UnsupportedOperationException("Unsupported format for tabular data: ${format}")
        }
    }

    private writeClinicalSubtask(SubtaskParameters parameters,
            TabularResultSerializer serializer,
            ImmutableList<MetadataAwareDataColumn> columns) {
        def stopWatch = new StopWatch("[Task ${parameters.task}] Write clinical data")
        stopWatch.start('Retrieve data')
        def args = new DataRetrievalParameters(
                constraint: parameters.constraint,
                sort: [new SortSpecification(dimension: 'patient')])
        def hypercube = multiDimService.retrieveClinicalData(args, parameters.user)
        stopWatch.stop()
        def tabularView = new SurveyTableView(columns, hypercube)
        try {
            log.info "[Task ${parameters.task}] Writing tabular data."
            stopWatch.start('Write data')
            serializer.writeParallel(tabularView, parameters.task)
        } finally {
            tabularView.close()
            stopWatch.stop()
            log.info "[Task ${parameters.task}] Writing clinical data completed.\n${stopWatch.prettyPrint()}"
        }
    }

    /**
     * Write clinical data to the output stream.
     * The columns are based on study-concept pairs, the rows represent patients.
     * If the input constraint is a conjunction of a patient set constraint with other constraints,
     * the query will be split into subqueries for subsets of the patient set. This spawns
     * parallel writer tasks. The results of those tasks are merged when all have finished.
     *
     * @param format
     * @param constraint
     * @param user
     * @param out
     * @param options
     */
    @Override
    void writeClinical(Format format,
                       DataRetrievalParameters parameters,
                       User user,
                       OutputStream out) {
        log.info "Start parallel export ..."
        List<HypercubeDataColumn> hypercubeColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                parameters.constraint, user)
        Boolean includeMeasurementDateColumns = parameters.includeMeasurementDateColumns
        final ImmutableList<MetadataAwareDataColumn> columns
        if (includeMeasurementDateColumns == null) {
            columns = surveyTableColumnService
                    .getMetadataAwareColumns(hypercubeColumns)
        } else {
            columns = surveyTableColumnService
                    .getMetadataAwareColumns(hypercubeColumns, includeMeasurementDateColumns)
        }
        final TabularResultSerializer serializer = getSerializer(format, user, (ZipOutputStream) out,
                ImmutableList.copyOf(columns as List<DataColumn>), parameters.exportFileName)

        def taskParameters = new TaskParameters(parameters.constraint, user)
        parallelPatientSetTaskService.run(
                taskParameters,
                {SubtaskParameters params -> writeClinicalSubtask(params, serializer, columns)},
                {taskResults -> serializer.combine()}
        )
    }

    @Override
    void writeTable(Format format, Constraint constraint, TableConfig tableConfig, User user, OutputStream out) {
        throw new UnsupportedOperationException("Writing tabular data for this view is not supported.")
    }

    @Override
    void writeTablePage(Format format, Constraint constraint, TableConfig tableConfig, PaginationParameters pagination, User user, OutputStream out) {
        throw new UnsupportedOperationException("Writing tabular data for this view is not supported.")
    }

    @Override
    void writeHighdim(Format format,
                      String type,
                      Constraint assayConstraint,
                      Constraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {
        throw new UnsupportedOperationException("Writing HD data for this view is not supported.")
    }

}
