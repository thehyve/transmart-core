package org.transmartproject.db.dataquery.highdim.vcf

import org.hibernate.ScrollableResults
import grails.orm.HibernateCriteriaBuilder
import org.hibernate.engine.SessionImplementor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.acgh.AcghDataTypeResource
import org.transmartproject.db.dataquery.highdim.acgh.AcghValuesProjection
import org.transmartproject.db.dataquery.highdim.acgh.DeSubjectAcghData
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.chromoregion.RegionRowImpl
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN

/**
 * Created by j.hudecek on 21-2-14.
 */
abstract class VcfModule extends AbstractHighDimensionDataTypeModule{
    static final String VCF_VALUES_PROJECTION = 'vcf_values'

    final List<String> platformMarkerTypes = ['Chromosomal']

    final String name = 'VCF'

    final String description = "VCF data"

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
        [
                standardDataConstraintFactory
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [
                new MapBasedParameterFactory(
                        (VCF_VALUES_PROJECTION): { Map<String, Object> params ->
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
                createCriteriaBuilder(deVariantSubjectDetails, 'vcf', session)

        criteriaBuilder.with {

            projections {
                property 'summary.chr'
                property 'summary.pos'
                property 'summary.subjectId'
                property 'summary.rsId'
                property 'summary.variant'
                property 'summary.variantFormat'
                property 'summary.variantType'
                property 'summary.reference'
                property 'summary.allele1'
                property 'summary.allele2'
            }

            order 'summary.chr', 'asc'
            order 'summary.pos', 'asc'
            order 'assay.id',  'asc' // important
        }

        criteriaBuilder
    }

}
