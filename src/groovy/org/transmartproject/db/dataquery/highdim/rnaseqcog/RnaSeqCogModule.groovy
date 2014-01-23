package org.transmartproject.db.dataquery.highdim.rnaseqcog

import com.google.common.collect.ImmutableSet
import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardAssayConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardDataConstraintFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN

/**
 * Module for RNA-seq, as implemented for Oracle by Cognizant.
 * This name is to distinguish it from the TraIT implementation.
 */
class RnaSeqCogModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'rnaseq_cog'

    final String description = "RNA Sequence data"

    final List<String> platformMarkerTypes = ['RNASEQ']

    final Set<String> dataProperties = ImmutableSet.of('rawIntensity', 'zscore')

    final Set<String> rowProperties = ImmutableSet.of('annotationId', 'geneSymbol', 'geneId')

    @Autowired
    StandardAssayConstraintFactory standardAssayConstraintFactory

    @Autowired
    StandardDataConstraintFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectRnaData, 'rnadata', session)

        criteriaBuilder.with {
            createAlias 'jAnnotation', 'ann', INNER_JOIN

            projections {
                property 'assay.id',         'assayId'

                property 'ann.id',           'annotationId'
                property 'ann.geneSymbol',   'geneSymbol'
                property 'ann.geneId',       'geneId'
            }

            order 'ann.id',         'asc'
            order 'ann.geneSymbol', 'asc'
            order 'assay.id',       'asc' // important! See assumption below

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {
        Map assayIndexMap = createAssayIndexMap assays

        def preliminaryResult = new DefaultHighDimensionTabularResult(
                rowsDimensionLabel:    'Transcripts',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    true,
                assayIdFromRow:        { it[0].assayId },
                inSameGroup:           { a, b -> a.annotationId == b.annotationId && a.geneSymbol == b.geneSymbol },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def firstNonNullCell = list.find()
                    new RnaSeqCogDataRow(
                            annotationId:  firstNonNullCell[0].annotationId,
                            geneSymbol:    firstNonNullCell[0].geneSymbol,
                            geneId:        firstNonNullCell[0].geneId,
                            assayIndexMap: assayIndexMap,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )

        new TabularResult<DataColumn, RnaSeqCogDataRow>() {
            @Delegate
            TabularResult<DataColumn, RnaSeqCogDataRow> delegate = preliminaryResult

            Iterator<RnaSeqCogDataRow> getRows() {
                new RepeatedAnnotationsCollectingIterator(delegate.iterator())
            }

            Iterator<RnaSeqCogDataRow> iterator() {
                getRows()
            }
        }
    }

    public static class RepeatedAnnotationsCollectingIterator
    extends AbstractIterator<RnaSeqCogDataRow> {

        PeekingIterator<RnaSeqCogDataRow> sourceIterator

        RepeatedAnnotationsCollectingIterator(Iterator<RnaSeqCogDataRow> sourceIterator) {
            this.sourceIterator = Iterators.peekingIterator sourceIterator
        }

        @Override
        protected RnaSeqCogDataRow computeNext() {
            List<RnaSeqCogDataRow> collected = []
            if (!sourceIterator.hasNext()) {
                endOfData()
                return
            }

            collected << sourceIterator.next()
            while (sourceIterator.hasNext() &&
                    sourceIterator.peek().annotationId != null &&
                    sourceIterator.peek().annotationId == collected[0].annotationId) {
                collected << sourceIterator.next()
            }

            if (collected.size() > 1) {
                collected[0].geneSymbol = collected*.geneSymbol.join('/')
            }
            collected[0]
        }
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ standardDataConstraintFactory,
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                        'GENE', 'jAnnotation', 'geneId') ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionsFactory(
                (Projection.DEFAULT_REAL_PROJECTION): 'rawIntensity',
                (Projection.ZSCORE_PROJECTION):       'zscore') ]
    }
}
