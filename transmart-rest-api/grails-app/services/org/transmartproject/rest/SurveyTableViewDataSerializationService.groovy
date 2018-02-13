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
import org.grails.core.util.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.SurveyTableColumnService
import org.transmartproject.db.multidimquery.HypercubeDataColumn
import org.transmartproject.db.multidimquery.SurveyTableView
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.support.ParallelPatientSetTaskService
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.rest.serialization.Format
import org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import java.util.zip.ZipOutputStream

import static org.transmartproject.db.support.ParallelPatientSetTaskService.SubtaskParameters
import static org.transmartproject.db.support.ParallelPatientSetTaskService.TaskParameters

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

    TabularResultSerializer getSerializer(Format format, User user, ZipOutputStream zipOutputStream, ImmutableList<DataColumn> columns) {
        switch(format) {
            case Format.TSV:
                return new TabularResultTSVSerializer(user, zipOutputStream, columns)
            case Format.SPSS:
                return new TabularResultSPSSSerializer(user, zipOutputStream, columns)
            default:
                throw new UnsupportedOperationException("Unsupported format for tabular data: ${format}")
        }
    }

    private writeClinicalSubtask(SubtaskParameters parameters,
            TabularResultSerializer serializer,
            ImmutableList<MetadataAwareDataColumn> columns,
            Dimension patientDimension) {
        def stopWatch = new StopWatch("[Task ${parameters.task}] Write clinical data")
        stopWatch.start('Retrieve data')
        def hypercube = multiDimService.retrieveClinicalData(parameters.constraint, parameters.user, [patientDimension])
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
     */
    @Override
    void writeClinical(Format format,
                       MultiDimConstraint constraint,
                       User user,
                       OutputStream out) {
        log.info "Start parallel export ..."
        List<HypercubeDataColumn> hypercubeColumns = surveyTableColumnService.getHypercubeDataColumnsForConstraint(
                (Constraint)constraint, user)
        final ImmutableList<MetadataAwareDataColumn> columns = ImmutableList.copyOf(
                surveyTableColumnService.getMetadataAwareColumns(hypercubeColumns))
        final TabularResultSerializer serializer = getSerializer(format, user, (ZipOutputStream) out,
                ImmutableList.copyOf(columns as List<DataColumn>))
        final patientDimension = multiDimService.getDimension('patient')

        def parameters = new TaskParameters((Constraint)constraint, user)
        parallelPatientSetTaskService.run(
                parameters,
                {SubtaskParameters params -> writeClinicalSubtask(params, serializer, columns, patientDimension)},
                {taskResults -> serializer.combine()}
        )
    }

    @Override
    void writeHighdim(Format format,
                      String type,
                      MultiDimConstraint assayConstraint,
                      MultiDimConstraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {
        throw new UnsupportedOperationException("Writing HD data for this view is not supported.")
    }

}
