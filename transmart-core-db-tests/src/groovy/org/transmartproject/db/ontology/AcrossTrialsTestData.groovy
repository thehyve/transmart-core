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

import org.transmartproject.db.concept.ConceptFullName

import static org.transmartproject.db.TestDataHelper.save

class AcrossTrialsTestData {

    List<ModifierDimensionCoreDb> modifierDimensions
    List<ModifierMetadataCoreDb> modifierMetadatas

    static AcrossTrialsTestData createDefault() {
        def list = []
        list << createModifier(path: '\\Demographics\\',
                code: 'CDEMO', nodeType: 'F')
        list << createModifier(path: '\\Demographics\\Age at Diagnosis\\',
                code: 'SNOMED:F-08104', nodeType: 'L', valueType: 'N', unit: 'year')
        list << createModifier(path: '\\Demographics\\Sex\\',
                code: 'SNOMED:F-03D86', nodeType: 'F')
        list << createModifier(path: '\\Demographics\\Sex\\Female\\',
                code: 'SNOMED:F-03CE5', nodeType: 'L')
        list << createModifier(path: '\\Demographics\\Sex\\Male\\',
                code: 'SNOMED:F-03CE6', nodeType: 'L')

        def result = new AcrossTrialsTestData()
        result.modifierDimensions = list*.get(0)
        result.modifierMetadatas = list*.get(1)

        result
    }

    /*
     * Properties:
     * - path
     * - code
     * - nodeType (L/F)
     * - name (optional, calculated)
     * - level (optional, calculated)
     * - studyId (optional)
     * - valueType (optional, N/T, calculated)
     * - unit (optional)
     */
    static List createModifier(Map<String, Object> properties) {
        if (['path', 'code', 'nodeType'].
                collect { properties."$it" == null }.any()) {
            throw new IllegalArgumentException("Missing required property")
        }
        if (!properties.name) {
            properties.name = new ConceptFullName(properties.path)[-1]
        }
        if (!properties.level) {
            properties.level = new ConceptFullName(properties.path).length - 1
        }
        if (!properties.valueType) {
            properties.valueType = 'T'
        }
        properties.visitInd = 'N'

        def dimension = new ModifierDimensionCoreDb(properties)
        def metadata = new ModifierMetadataCoreDb(properties)

        [dimension, metadata]
    }


    void saveAll() {
        save modifierDimensions
        save modifierMetadatas
    }
}
