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

package org.transmartproject.db.ontology.http

import grails.test.mixin.TestFor
import org.transmartproject.db.http.BusinessExceptionResolver
import spock.lang.Specification

import static org.hamcrest.Matchers.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(BusinessExceptionController)
class BusinessExceptionControllerSpec extends Specification {

    void basicTest() {
        request.setAttribute(BusinessExceptionResolver
                .REQUEST_ATTRIBUTE_STATUS, 403)
        request.setAttribute(BusinessExceptionResolver
                .REQUEST_ATTRIBUTE_EXCEPTION, new RuntimeException('foo'))

        controller.index()

        expect:
        response.status == 403
        response.text.contains('foo')
        response.text.contains('RuntimeException')
    }
}
