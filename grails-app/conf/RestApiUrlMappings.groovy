/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

class RestApiUrlMappings {

    static mappings = {
        '/studies'(controller: 'study', method: 'GET', resources: 'study', includes: ['index', 'show'])

        '/studies'(resources: 'study', method: 'GET') {
            '/subjects'(controller: 'subject', resources: 'subject', includes: ['index', 'show'])
        }

        "/studies/$studyId/concepts"(
                controller: 'concept', action: 'index'
        )

        "/studies/$studyId/concepts/$id**"(
                controller: 'concept', action: 'show'
        )

        "/studies/$studyId/concepts/$conceptId**/subjects"(
                controller: 'subject', action: 'indexByConcept'
        )

        "/studies/$studyId/concepts/$conceptId**/observations"(
                controller: 'observation', action: 'indexByConcept'
        )

        "/studies/$studyId/concepts/$conceptId**/highdim"(
                controller: 'highDim', action: 'show'
        )

        '/studies'(resources: 'study', method: 'GET') {
            '/observations'(controller: 'observation', resources: 'observation', includes: ['index'])
        }

        '/studies'(resources: 'study', method: 'GET') {
            '/subjects'(resources: 'subject', method: 'GET') {
                '/observations'(controller: 'observation', action:'indexBySubject')
            }
        }

    }
}
