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

package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.SessionFactory
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.querytool.ConstraintByOmicsValue
import org.transmartproject.core.querytool.ConstraintByOmicsValue.Operator
import org.transmartproject.core.querytool.HighDimensionFilterType

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
     * Returns a list of markertypes that are supported by this module
     *
     * See {@link HighDimensionDataTypeResource#matchesPlatform(Platform)}.
     * See {@link DeGplInfo#getMarkerType()}.
     *
     * @return List of marker types supported by this module
     */
    List<String> getPlatformMarkerTypes()

    /**
     * Search through the annotations of a given concept_code, for entries in search_property
     * starting with search_term.
     * @param concept_code
     * @param search_term
     * @param search_property
     * @return An alphabetical list of annotations that start with search_term (case insensitive match),
     * or an empty list if the search_property is unsupported
     */
    List<String> searchAnnotation(String concept_code, String search_term, String search_property)

    /**
     * @return A list of properties that can be used as search_property in {@link #searchAnnotation(String, String, String)}.
     */
    List<String> getSearchableAnnotationProperties()

    /**
     * @return A list of projections supported for filtering assays (e.g. to be used in cohort selection)
     */
    List<String> getSearchableProjections()

    /**
     * Finds the distribution for a particular property of a high dimensional dataset (e.g. the expression levels in log intensity of the probes associated with KRAS gene)
     * @param constraint A {@link ConstraintByOmicsValue} object with at least selector, property and projectionType fields set to non-null values.
     * The projectionType should correspond to a member of the list {@link #getSearchableProjections()}, e.g. 'logIntensity'
     * The property should be a valid property of this high-dimensional data, i.e. it should be a member of the list {@link #getSearchableAnnotationProperties()}, e.g. 'geneSymbol'
     * The selector is used to match a property against, e.g. 'KRAS'
     * @param concept_code The concept code associated with the high dimensional data
     * @param result_instance_id If this is null it will be ignored. Otherwise only the values for patients that are members
     * of the given result instance will be returned.
     *
     * @return A list of items, each item is a list where the first element is the patient id and the second element is the
     * associated value in the given projection.
     */
    def getDistribution(ConstraintByOmicsValue constraint, String concept_code, Long result_instance_id)

    /**
     * Get the {@link HighDimensionFilterType} for this high dimension data type.
     * @return the filter type.
     */
    HighDimensionFilterType getHighDimensionFilterType()
}
