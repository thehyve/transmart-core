package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.SessionFactory
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

public interface HighDimensionDataTypeModule {

    /**
     * The name of the data type supported by this module.
     * @return
     */
    String getName()

    /**
     * A human-readable description of this datatype
     * @return
     */
    String getDescription()

    /**
     * The session factory used by this module.
     * @return
     */
    SessionFactory getSessionFactory()

    /**
     * A set of assay constraints supported by this data type/
     */
    Set<String> getSupportedAssayConstraints()

    /**
     * A set of data constraints supported by this data type.
     */
    Set<String> getSupportedDataConstraints()

    /**
     * A set of projections supported by this data type,.
     */
    Set<String> getSupportedProjections()

    /**
     * Creates an assay constraint from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name)

    /**
     * Creates a data constraint from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    DataConstraint createDataConstraint(Map<String, Object> params, String name)

    /**
     * Creates a projection from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    Projection createProjection(Map<String, Object> params, String name)

    /**
     * Prepares the Criteria-based query to be issued. The data constraints will
     * have the opportunity to modify the criteria before it is issued.
     * @param projection
     * @return
     */
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session)

    /**
     * Transform the query result into the final object to be returned by
     * {@link HighDimensionDataTypeResource#retrieveData(List, List, Projection)}.
     */
    TabularResult transformResults(ScrollableResults results,
                                     List<AssayColumn> assays,
                                     Projection projection)

    /**
     * Whether the platform passed in refers to data of the type implemented by
     * this module.
     *
     * See {@link HighDimensionDataTypeResource#matchesPlatform(Platform)}.
     *
     * @param platform the platform to match
     * @return true iif the the platform and this data type match
     */
    boolean matchesPlatform(Platform platform)
}
