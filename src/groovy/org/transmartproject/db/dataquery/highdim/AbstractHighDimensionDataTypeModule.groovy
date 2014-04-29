package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.SessionFactory
import org.hibernate.engine.SessionImplementor
import org.hibernate.impl.CriteriaImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory

import javax.annotation.PostConstruct

abstract class AbstractHighDimensionDataTypeModule implements HighDimensionDataTypeModule {

    @Autowired
    SessionFactory sessionFactory

    protected List<DataRetrievalParameterFactory> assayConstraintFactories

    protected List<DataRetrievalParameterFactory> dataConstraintFactories

    protected List<DataRetrievalParameterFactory> projectionFactories

    @Autowired
    HighDimensionResourceService highDimensionResourceService

    static Map<String, Class> typesMap(Class domainClass, List<String> fields,
                                       Map<String, String> translationMap = [:]) {
        fields.collectEntries({
            [(it): domainClass.metaClass.getMetaProperty(translationMap.get(it, it)).type]
        }).asImmutable()
    }

    @PostConstruct
    void init() {
        this.highDimensionResourceService.registerHighDimensionDataTypeModule(
                name, this.&createHighDimensionResource)
    }

    HighDimensionDataTypeResource createHighDimensionResource(Map params) {
        /* params are unused; at least for now */
        new HighDimensionDataTypeResourceImpl(this)
    }

    @Lazy volatile Set<String> supportedAssayConstraints = {
        initializeFactories()
        assayConstraintFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    @Lazy volatile Set<String> supportedDataConstraints = {
        initializeFactories()
        dataConstraintFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    @Lazy volatile Set<String> supportedProjections = {
        initializeFactories()
        projectionFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    final synchronized protected initializeFactories() {
        if (assayConstraintFactories != null) {
            return // already initialized
        }

        assayConstraintFactories = createAssayConstraintFactories()
        dataConstraintFactories  = createDataConstraintFactories()
        projectionFactories      = createProjectionFactories()
    }

    abstract protected List<DataRetrievalParameterFactory> createAssayConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createDataConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createProjectionFactories()

    @Override
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in assayConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createAssayConstraint)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the assay constraint $name")
    }

    @Override
    DataConstraint createDataConstraint(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in dataConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createDataConstraint)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the data constraint $name")
    }

    @Override
    Projection createProjection(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in projectionFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createProjection)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the projection $name")
    }

    final protected HibernateCriteriaBuilder createCriteriaBuilder(
            Class targetClass, String alias, SessionImplementor session) {

        HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(targetClass, sessionFactory)

        /* we have to write a private here */
        if (session) {
            //force usage of a specific session (probably stateless)
            builder.criteria = new CriteriaImpl(targetClass.canonicalName,
                                                alias,
                                                session)
            builder.criteriaMetaClass = GroovySystem.metaClassRegistry.
                    getMetaClass(builder.criteria.getClass())
        } else {
            builder.createCriteriaInstance()
        }

        /* builder.instance.is(builder.criteria) */
        builder.instance.readOnly = true
        builder.instance.cacheable = false

        builder
    }

    final protected Map createAssayIndexMap(List assays) {
        int i = 0
        assays.collectEntries { [ it, i++ ] }
    }

    abstract protected List<String> getPlatformMarkerTypes()

    @Override
    boolean matchesPlatform(Platform platform) {
        platform.markerType in platformMarkerTypes
    }
}
