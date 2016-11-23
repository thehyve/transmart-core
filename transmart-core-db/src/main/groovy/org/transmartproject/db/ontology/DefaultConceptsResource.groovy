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

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.concept.ConceptKey

/**
 * Handles loading {@link OntologyTerm}s from <code>table_access</code> and
 * related tables.
 */
class DefaultConceptsResource implements ConceptsResource {

    @Override
    List<OntologyTerm> getAllCategories() {
        TableAccess.getCategories(true, true)
    }

    @Override
    OntologyTerm getByKey(String conceptKey) throws NoSuchResourceException {
        def ck, domainClass, result
        ck = new ConceptKey(conceptKey)
        domainClass = TableAccess.findByTableCode(ck.tableCode)?.
                ontologyTermDomainClassReferred
        if (!domainClass)
            throw new NoSuchResourceException("Unknown or unmapped table " +
                    "code: $ck.tableCode")
        result = domainClass.findByFullNameAndCSynonymCd(ck.conceptFullName
                .toString(), 'N' as Character)
        if (!result) {
            throw new NoSuchResourceException('No non-synonym concept with ' +
                    "fullName '$ck.conceptFullName' found for type " +
                    "$domainClass")
        }
        result.setTableCode(ck.tableCode)
        result
    }
}
