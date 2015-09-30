/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.hibernate.FetchMode
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.SimpleTabularResult
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.dataconstraints.PropertyDataConstraint
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN
import static org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN
import static org.transmartproject.core.dataquery.highdim.projections.Projection.ALL_DATA_PROJECTION
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.processStringList
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.validateParameterNames

/**
 * Module for SNP data, as implemented in the Pfizer branch.
 * Data is stored in DE_SNP_DATA_BY_PROBE.. The columns GPS_BY_PROBE_BLOB,
 * GTS_BY_PROBE_BLOB and DOSE_BY_PROBE_BLOB were compressed with
 * UTL_COMPRESS.LZ_COMPRESS and store the data space separated, each token
 * referring to a specific patient in the same order indicated by
 * DE_SNP_SUBJECT_SORTED_DEF.PATIENT_POSITION.
 */
@Log4j
class SnpLzModule extends AbstractHighDimensionDataTypeModule {

    private static final int SNP_PATIENTS_FETCH_SIZE = 5000
    public static final String SNPS_CONSTRAINT_NAME = 'snps'

    final String name = 'snp_lz'

    final String description = "SNP data (Gzip compressed)"

    final List<String> platformMarkerTypes = ['SNP']

    final Map<String, Class> dataProperties = typesMap(SnpLzCell,
            ['probabilityA1A1', 'probabilityA1A2', 'probabilityA2A2',
             'likelyAllele1', 'likelyAllele2', 'minorAlleleDose'])

    final Map<String, Class> rowProperties = typesMap(SnpLzRow,
            ['snpName', 'a1', 'a2', 'imputeQuality', 'GTProbabilityThreshold',
             'minorAlleleFrequency', 'minorAllele', 'a1a1Count', 'a1a2Count',
             'a2a2Count', 'noCallCount'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Override
    HighDimensionDataTypeResource createHighDimensionResource(Map params) {
        new SnpLzHighDimensionDataTypeResource(this)
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [standardAssayConstraintFactory]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        chromosomeSegmentConstraintFactory.with {
            segmentPrefix      = 'ann.'
            segmentStartColumn = 'pos'
            segmentEndColumn   = 'pos'
            forceBigDecimal    = true
        }

        [standardDataConstraintFactory,
         chromosomeSegmentConstraintFactory,
         new MapBasedParameterFactory(
                 (SNPS_CONSTRAINT_NAME): { Map<String, Object> params ->
                     validateParameterNames(['names'], params)
                     new PropertyDataConstraint(
                             property: 'ann.snpName',
                             values:    processStringList('names', params.names))
                 },
                 (DataConstraint.GENES_CONSTRAINT): { Map<String, Object> params ->
                     if (params['ids']) {
                         throw new InvalidArgumentsException("Giving genes " +
                                 "by search keyword ids is not supported " +
                                 "for this data type (given: ${params['ids']}")
                     }
                     validateParameterNames(['names'], params)
                     new PropertyDataConstraint(
                             property: 'rcSnpInfo.geneName',
                             values: processStringList('names', params.names))
                 }
         ),
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [
                new MapBasedParameterFactory(
                        (ALL_DATA_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new SnpLzAllDataProjection()
                        }
                ),
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(SnpDataByProbeCoreDb, 'snp', session)

        criteriaBuilder.with {
            createAlias 'genotypeProbeAnnotation', 'ann',       INNER_JOIN
            createAlias 'jRcSnpInfo',              'rcSnpInfo', LEFT_OUTER_JOIN

            projections {
                property 'snp.a1',                     'a1'
                property 'snp.a2',                     'a2'
                property 'snp.a1Clob',                 'a1Clob'
                property 'snp.a2Clob',                 'a2Clob'
                property 'snp.imputeQuality',          'imputeQuality'
                property 'snp.gpsByProbeBlob',         'gpsByProbeBlob'
                property 'snp.gtsByProbeBlob',         'gtsByProbeBlob'
                property 'snp.doseByProbeBlob',        'doseByProbeBlob'
                property 'snp.gtProbabilityThreshold', 'gtProbabilityThreshold'
                property 'snp.maf',                    'maf'
                property 'snp.minorAllele',            'minorAllele'
                property 'snp.CA1A1',                  'CA1A1'
                property 'snp.CA1A2',                  'CA1A2'
                property 'snp.CA2A2',                  'CA2A2'
                property 'snp.CNocall',                'CNocall'
                property 'snp.trialName',              'trialName'

                property 'ann.geneInfo',               'geneInfo'
                property 'ann.snpName',                'snpName'
            }

            /* We need to add this restriction, otherwise we get duplicated rows
             * The version doesn't really matter, we only want the mapping
             * between genes and snp_id. So we should choose the version that
              * has more and more accurate of these mappings */
            eq 'rcSnpInfo.hgVersion', '19'

            order 'ann.id', 'asc'

            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results,
                                   List<AssayColumn> assays,
                                   Projection projection) {

        def trials = [] as Set
        assays.each { trials << it.trialName }
        log.debug "Set of trials for assays: $trials"

        if (trials.size() == 0) {
            throw new EmptySetException(
                    "Assay set $trials has no trial information")
        } else if (trials.size() > 1) {
            // Do not permit more than one trial, otherwise we wouldn't be able
            // to returns meaningful row properties (or we'd have to make them
            // maps)
            throw new UnexpectedResultException("Found more than one trial in " +
                    "the assay set; this is not allowed for this data type")
        }

        // obtain patient order for all of the trials of the result
        def sssList = SnpSubjectSortedDef.createCriteria().list {
            eq 'trialName', trials.first()
            order 'patientPosition', 'asc'

            fetchSize SNP_PATIENTS_FETCH_SIZE
            fetchMode 'patient', FetchMode.JOIN
        }
        log.debug "Found ${sssList.size()} subject ordering entries for assays $assays"

        LinkedHashMap<AssayColumn, Integer> assayIndexMap = [:] as LinkedHashMap
        Set<AssayColumn> foundAssays = [] as Set

        def assayForPatientId = assays.collectEntries { [it.patient.id, it] }
        // map assays to their positions in the blobs
        sssList.each { SnpSubjectSortedDef sss ->
            AssayColumn assay = assayForPatientId[sss.patient.id]
            if (!assay) {
                log.trace "SnpSubjectSortedDef entry $sss not matched to any selected assay"
                return
            }

            foundAssays << assay

            assert assayIndexMap[assay] == null : "Not there yet"
            assert sss.patientPosition > 0 : 'patient positions are 1-based in the DB'
            assayIndexMap[assay] = sss.patientPosition - 1 // make it 0-based
        }

        // have we found the positions for all the assays?
        def assaysNotFound = (assays as Set) - foundAssays
        if (assaysNotFound) {
            throw new UnexpectedResultException(
                    "Could not find the blob position for " +
                            "${assaysNotFound.size()} assays: $assaysNotFound")
        }

        def transformer = new SnpLzRowTransformer(sssList.size(), assayIndexMap)

        new SimpleTabularResult(
                rowsDimensionLabel:    'SNPs',
                columnsDimensionLabel: 'Sample codes',
                indicesList:            assays,
                results:                results,
                convertDbRow:           transformer.&transformDbRow)
    }

    @CompileStatic
    static class SnpLzRowTransformer {
        private int numPatientsInTrial
        private LinkedHashMap<AssayColumn, Integer> assayIndexMap
        private int[] resultIndexPatientIndex

        SnpLzRowTransformer(
                int numPatientsInTrial,
                LinkedHashMap<AssayColumn, Integer> orderedAssayPatientIndex) {

            this.numPatientsInTrial = numPatientsInTrial
            this.assayIndexMap = orderedAssayPatientIndex
            this.resultIndexPatientIndex =
                    orderedAssayPatientIndex.values() as int[]
        }

        SnpLzRow transformDbRow(Object[] rawRow) {
            Map probeData = (Map) rawRow[0]

            new SnpLzRow(
                    probeData,
                    numPatientsInTrial,
                    assayIndexMap,
                    resultIndexPatientIndex)
        }
    }

}
