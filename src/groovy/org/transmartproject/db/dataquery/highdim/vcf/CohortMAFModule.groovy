package org.transmartproject.db.dataquery.highdim.vcf

import org.hibernate.ScrollableResults
import grails.orm.HibernateCriteriaBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.vcf.*
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import static org.hibernate.sql.JoinFragment.INNER_JOIN
/**
 * Created by j.hudecek on 6-2-14.
 */
class CohortMAFModule extends AbstractHighDimensionDataTypeModule {

    static final String VALUES_PROJECTION = 'cohortMAF_values'

    final List<String> platformMarkerTypes = ['Chromosomal']

    final String name = 'cohortMAF'

    final String description = "cohortMAF_values data"

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Override
    HighDimensionDataTypeResource createHighDimensionResource(Map params) {


        /* return instead subclass of HighDimensionDataTypeResourceImpl,
         * because we add a method, retrieveChromosomalSegments() */
        new HighDimensionDataTypeResourceImpl(this)
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        //customize the segment constraint factory to produce constraints targeting the right DeVariantSubjectSummaryCoreDb columns
        chromosomeSegmentConstraintFactory.segmentStartColumn = 'pos'
        chromosomeSegmentConstraintFactory.segmentEndColumn = 'pos'
        chromosomeSegmentConstraintFactory.segmentChromosomeColumn = 'chr'
        chromosomeSegmentConstraintFactory.regionPrefix = 'jDetail.'
        [
                standardDataConstraintFactory,
                chromosomeSegmentConstraintFactory
                //TODO: implement constraint on dataset
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ //there needs to be a projection factory even though we're not using projections
                new MapBasedParameterFactory(
                        (VALUES_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new VcfValuesProjection()
                        }
                )
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(DeVariantSubjectSummaryCoreDb, 'summary', session)

        criteriaBuilder.with {
            createAlias 'jDetail', 'p', INNER_JOIN
            projections {
                property 'summary.subjectId'
                property 'summary.rsId'
                property 'summary.variant'
                property 'summary.variantFormat'
                property 'summary.variantType'
                property 'summary.reference'
                property 'summary.allele1'
                property 'summary.allele2'

                property 'p.chr'
                property 'p.pos'
                property 'p.rsId'
                property 'p.ref'
                property 'p.alt'
                property 'p.quality'
                property 'p.filter'
                property 'p.info'
                property 'p.format'
                property 'p.variant'

                property 'assay.id'
            }

            order 'assay.id',  'asc' // important
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results,
                                   List<AssayColumn> assays,
                                   Projection projection) {
        /* assumption here is the assays in the passed in list are in the same
         * order as the assays in the result set */
        Map assayIndexMap = createAssayIndexMap assays

        new DefaultHighDimensionTabularResult(
                rowsDimensionLabel:    'Regions',
                columnsDimensionLabel: 'Sample codes',
                allowMissingAssays:    true,
                indicesList:           assays,
                results:               results,
                assayIdFromRow:        { it[18] } , //18th column is the assay.id
                inSameGroup:           { a, b -> a[10] == b[10] && a[9] == b[9] /* chr, pos*/ },
                finalizeGroup:         { List collectedEntries -> /* list of all the results belonging to a group defined by inSameGroup */
                    collectedEntries = collectedEntries - null
                    DeVariantSubjectDetailCoreDb summary = initSubjectDetail(collectedEntries)
                    Map alleleDistribution = getAlleleDistribution(collectedEntries)
                    return calculateVcfValues(alleleDistribution, summary, collectedEntries)
                }
        )
    }

    private Map getAlleleDistribution(List collectedEntries) {
        def alleleDistribution = [:].withDefault { 0 }
        for (row in collectedEntries) {
            if (row == null)
                continue;
            def allele1 = row[6]
            def allele2 = row[7]
            alleleDistribution[allele1]++
            alleleDistribution[allele2]++
        }
        alleleDistribution
    }

    private DeVariantSubjectDetailCoreDb initSubjectDetail(List collectedEntries) {
        def summary = new DeVariantSubjectDetailCoreDb([
                'chr': collectedEntries[0][8],
                'pos': collectedEntries[0][9],
                'rsId': collectedEntries[0][10],
                'ref': collectedEntries[0][11],
                'alt': collectedEntries[0][12],
                'quality': collectedEntries[0][13],
                'filter': collectedEntries[0][14],
                'info': collectedEntries[0][15],
                'format': collectedEntries[0][16],
                'variant': collectedEntries[0][17]])
        summary
    }

    private VcfValuesImpl calculateVcfValues(Map alleleDistribution, DeVariantSubjectDetailCoreDb summary, List collectedEntries) {
        Map assayIndexMap
        if (!alleleDistribution) return null

        int total = alleleDistribution.values().sum()
        def altAlleleNums = alleleDistribution.keySet() - [DeVariantSubjectSummaryCoreDb.REF_ALLELE]

        if (!altAlleleNums) return null

        def altAlleleDistribution = alleleDistribution.subMap(altAlleleNums)
        def altAlleleFrequencies = altAlleleDistribution.collectEntries { [(it.key): it.value / (double) total] }
        def mafEntry = altAlleleFrequencies.max { it.value }

        def additionalInfo = [:]
        additionalInfo['AC'] = altAlleleDistribution.values().join(',')
        additionalInfo['AF'] = altAlleleFrequencies.values().collect { String.format('%.2f', it) }.join(',')
        additionalInfo['AN'] = total.toString()
        additionalInfo['VC'] = summary.additionalInfo['VC']

        def altAlleles = summary.getAltAllelesByPositions(altAlleleNums)
        def mafAllele = altAlleles[altAlleleNums.asList().indexOf(mafEntry.key)]
        def genomicVariantTypes = summary.getGenomicVariantTypes(altAlleles)

        VcfValuesImpl ret = new VcfValuesImpl([
                chromosome: summary.chromosome,
                position: summary.position,
                rsId: summary.rsId,
                mafAllele: mafAllele,
                maf: mafEntry.value,
                qualityOfDepth: summary.qualityOfDepth,
                referenceAllele: summary.referenceAllele,
                alternativeAlleles: altAlleles,
                additionalInfo: additionalInfo,
                genomicVariantTypes: genomicVariantTypes,
                assayIndexMap: assayIndexMap,
                data: collectedEntries,
                details: [summary]
        ]
        )


        return ret;
    }

}
