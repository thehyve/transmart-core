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

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.hibernate.FetchMode
import org.hibernate.ScrollableResults
import org.hibernate.engine.spi.SessionImplementor
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
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.db.dataquery.SimpleTabularResult
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.dataconstraints.PropertyDataConstraint
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory

import static org.hibernate.sql.JoinType.INNER_JOIN
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

    private static final String SNPS_CONSTRAINT_NAME = 'snps'

    private static final String ALLELES_PROJECTION = 'alleles'
    private static final String PROBABILITIES_PROJECTION = 'probabilities'
    private static final String DOSE_PROJECTION = 'dose'


    final String name = 'snp_lz'

    final String description = "SNP data (Gzip compressed)"

    final List<String> platformMarkerTypes = ['SNP']

    final Map<String, Class> dataProperties = typesMap(SnpLzAllDataCell,
            ['probabilityA1A1', 'probabilityA1A2', 'probabilityA2A2',
             'likelyAllele1', 'likelyAllele2', 'minorAlleleDose'])

    final Map<String, Class> rowProperties = typesMap(SnpLzRow,
            ['snpName', 'chromosome', 'position', 'a1', 'a2', 'imputeQuality',
             'GTProbabilityThreshold', 'minorAlleleFrequency', 'minorAllele',
             'a1a1Count', 'a1a2Count', 'a2a2Count', 'noCallCount'])

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
            forceBigDecimal    = false
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
                     new SnpGeneNameConstraint(
                             property: 'ann.snpName',
                             geneNames: processStringList('names', params.names))
                 }
         ),
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        def map = [
                (ALL_DATA_PROJECTION):      SnpLzAllDataProjection,
                (ALLELES_PROJECTION):       SnpLzAllelesProjection,
                (PROBABILITIES_PROJECTION): SnpLzProbabilitiesProjection,
                (DOSE_PROJECTION):          SnpLzDoseProjection,
        ].collectEntries { k, v ->
            [k, { Map<String, Object> params ->
                if (!params.isEmpty()) {
                    throw new InvalidArgumentsException('Expected no parameters here')
                }
                v.newInstance()
            }]
        }

        [new MapBasedParameterFactory(map)]
    }

    /**
     * Get the trial name from a collection of assays.
     * The assays should belong to exactly one trial, since the
     * snp_lz rows contain data for all subjects in a trial.
     * @param assays the collection of assays for which the data
     *  is queried.
     * @throws EmptySetException iff the collection contains no trial names
     *  (i.e., the set of assays is empty).
     * @throws UnexpectedResultException iff the collection multiple trial
     *  names.
     * @return the trial name.
     */
    String getTrialNameFromAssays(Collection<AssayColumn> assays) {
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
        return trials[0]
    }

    /**
     * Get the platform id from a collection of assays.
     * The assays should belong to exactly one platform, since the
     * snp_lz rows contain data for all subjects in a trial.
     * @param assays the collection of assays for which the data
     *  is queried.
     * @throws EmptySetException iff the collection contains no platform ids
     *  (i.e., the set of assays is empty).
     * @throws UnexpectedResultException iff the collection multiple platform ids.
     * @return the platform id.
     */
    String getBioAssayPlatformIdFromAssays(Collection<AssayColumn> assays) {
        def platforms = [] as Set
        assays.each { platforms << it.platform.id }
        log.debug "Set of platforms for assays: $platforms"

        if (platforms.size() == 0) {
            throw new EmptySetException(
                    "Assay set $platforms has no platform information")
        } else if (platforms.size() > 1) {
            // Do not permit more than one platform, otherwise we wouldn't be able
            // to returns meaningful row properties.
            throw new UnexpectedResultException("Found more than one platform in " +
                    "the assay set; this is not allowed for this data type")
        }
        return platforms[0]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(
        Projection projection,
        SessionImplementor session) {
        throw new UnsupportedByDataTypeException("The snp_lz data module requires " +
            "the list of assays to be specified when querying data.")
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(
        List<AssayColumn> assays,
        Projection projection,
        SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(SnpDataByProbeCoreDb, 'snp', session)

        def trialName = getTrialNameFromAssays(assays)
        log.debug "Add constraint for trailName '${trialName}'"

        def platformId = getBioAssayPlatformIdFromAssays(assays)
        log.debug "Add constraint for platformId '${platformId}'"

        criteriaBuilder.with {
            createAlias 'genotypeProbeAnnotation', 'ann',       INNER_JOIN
            createAlias 'bioAssayGenoPlatform',    'assay_platform',  INNER_JOIN
            createAlias 'assay_platform.bioAssayPlatform',  'platform',  INNER_JOIN

            projections {
                property 'snp.a1',                     'a1'
                property 'snp.a2',                     'a2'
                property 'snp.a1Clob',                 'a1Clob'
                property 'snp.a2Clob',                 'a2Clob'
                property 'snp.imputeQuality',          'imputeQuality'
                property 'snp.gtProbabilityThreshold', 'gtProbabilityThreshold'
                property 'snp.maf',                    'maf'
                property 'snp.minorAllele',            'minorAllele'
                property 'snp.countA1A1',              'countA1A1'
                property 'snp.countA1A2',              'countA1A2'
                property 'snp.countA2A2',              'countA2A2'
                property 'snp.countNocall',            'countNocall'
                property 'snp.trialName',              'trialName'

                property 'ann.geneInfo',               'geneInfo'

                property 'ann.snpName',                'snpName'
                property 'ann.chromosome',             'chromosome'
                property 'ann.pos',                    'position'

                property 'platform.accession',         'platformId'
            }

            /*
             * This constraint is required, since rows in the {@link SnpDataByProbeCoreDb}
             * table are not associated with a single subject, but with all the
             * subjects in a particular trial.
             */
            eq 'snp.trialName', trialName

            /*
             * This constraint is required in the case there is SNP data for multiple
             * platforms within a trial.
             */
            eq 'platform.accession', platformId

            /*
             * Constraint required to prevent duplicates when multiple genome
             * builds are present in the genotypeProbeAnnotation table.
             */
            eq 'ann.genomeBuild', 'GRCh37'

            order 'ann.id', 'asc'

            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override //@CompileStatic
    TabularResult transformResults(ScrollableResults results,
                                   List<AssayColumn> assays,
                                   Projection projection) {

        def trials = [] as Set
        def platforms = [] as Set
        assays.each {
            trials << it.trialName
            platforms << it.platform.id
        }
        log.debug "Set of trials for assays: $trials"
        log.debug "Set of platforms for assays: $platforms"

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

        if (platforms.size() == 0) {
            throw new EmptySetException(
                    "Assay set $platforms has no platform information")
        } else if (platforms.size() > 1) {
            // Do not permit more than one platform, otherwise we wouldn't be able
            // to returns meaningful row properties (or we'd have to make them
            // maps)
            throw new UnexpectedResultException("Found more than one platform in " +
                    "the assay set; this is not allowed for this data type")
        }

        log.debug "Querying patient order for trial '${trials.first()}' and platform '${platforms.first()}'."
        // obtain patient order for all of the trials of the result
        def sssList = SnpSubjectSortedDef.createCriteria().list {
            createAlias 'bioAssayPlatform',  'platform',  INNER_JOIN

            eq 'trialName', trials.first()
            eq 'platform.accession', platforms.first()
            order 'patientPosition', 'asc'

            fetchSize SNP_PATIENTS_FETCH_SIZE
            fetchMode 'patient', FetchMode.JOIN
        } as List<SnpSubjectSortedDef>
        log.debug "Found ${sssList.size()} subject ordering entries for assays."

        LinkedHashMap<AssayColumn, Integer> assayIndexMap = [:] as LinkedHashMap
        Set<AssayColumn> assaysNotFound = [] as Set

        /*
         * Map assays to their positions in the blobs.
         * The ordering of the <var>assays</var> list is preserved in the iterator of assayIndexMap.
         */
        def sssForPatientId = sssList.collectEntries { [it.patient.id, it] }
        assays.each { AssayColumn assay ->
            SnpSubjectSortedDef sss = sssForPatientId[assay.patient.id]
            if (!sss) {
                assaysNotFound << assay
            }
            assert assayIndexMap[assay] == null : "Not there yet"
            assert sss.patientPosition > 0 : 'patient positions are 1-based in the DB'
            assayIndexMap[assay] = sss.patientPosition - 1 // make it 0-based
        }
        if (assaysNotFound) {
            throw new UnexpectedResultException(
                    "Could not find the blob position for " +
                            "${assaysNotFound.size()} assays: $assaysNotFound")
        }

        def transformer = new SnpLzRowTransformer(
                sssList.size(), assayIndexMap, projection)

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
        Projection projection

        SnpLzRowTransformer(
                int numPatientsInTrial,
                LinkedHashMap<AssayColumn, Integer> orderedAssayPatientIndex,
                Projection projection) {

            this.numPatientsInTrial = numPatientsInTrial
            this.assayIndexMap = orderedAssayPatientIndex
            this.resultIndexPatientIndex =
                    orderedAssayPatientIndex.values() as int[]
            this.projection = projection
        }

        SnpLzRow transformDbRow(Object[] rawRow) {
            Map probeData = (Map) rawRow[0]

            new SnpLzRow(
                    probeData,
                    numPatientsInTrial,
                    assayIndexMap,
                    resultIndexPatientIndex,
                    projection)
        }
    }

}
