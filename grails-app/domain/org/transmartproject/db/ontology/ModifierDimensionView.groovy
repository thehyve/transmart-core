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

package org.transmartproject.db.ontology

/**
 * View that joins {@link ModifierDimensionView} and
 * {@link ModifierMetadataCoreDb}. It has to be done on a view because
 * modifier_metadata inexplicably doesn't use modifier_dimension's primary key,
 * though modifier_cd functions as a de facto second primary key.
 */
class ModifierDimensionView {

    // it's a view!

    String path
    String code
    String name
    Long   level
    String studyId
    String nodeType
    String valueType
    String unit
    String visitInd = 'N'

    static transients = ['visit']

    Boolean visit

    Boolean isVisit() {
        visitInd == 'Y'
    }

    void setVisit(Boolean visit) {
        visitInd == visit == null ? null :
                visit ? 'Y' : 'N'
    }

    static mapping = {
        table   schema: 'i2b2demodata', name: 'modifier_dimension_view'
        id      name: 'path', generator: 'assigned'
        version false

        path      column: 'modifier_path'
        code      column: 'modifier_cd'
        name      column: 'name_char'
        level     column: 'modifier_level'
        studyId   column: 'sourcesystem_cd'
        nodeType  column: 'modifier_node_type' // known values: {L, F}
        valueType column: 'valtype_cd'
        unit      column: 'std_units'
    }
}
