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

    // grails url-mappings-report can come handy here...

    static mappings = {

        '/businessException/index'(controller: 'businessException', action: 'index')

        group "/v2", {
            "/observation_list"(method: 'GET', controller: 'query', action: 'observationList') {
                apiVersion = 'v2'
            }
            "/observations"(method: 'GET', controller: 'query', action: 'observations') {
                apiVersion = 'v2'
            }
            "/high_dim"(method: 'GET', controller: 'query', action: 'highDim') {
                apiVersion = 'v2'
            }
            "/supported_fields"(method: 'GET', controller: 'query', action: 'supportedFields') {
                apiVersion = 'v2'
            }
            "/observations/aggregate"(method: 'GET', controller: 'query', action: 'aggregate') {
                apiVersion = 'v2'
            }
            "/observations/count"(method: 'GET', controller: 'query', action: 'count') {
                apiVersion = 'v2'
            }
            "/patient_sets/$id"(method: 'GET', controller: 'patientQuery', action: 'findPatientSet') {
                apiVersion = 'v2'
            }
            "/patient_sets"(method: 'POST', controller: 'patientQuery', action: 'createPatientSet') {
                apiVersion = 'v2'
            }
            "/patients/$id"(method: 'GET', controller: 'patientQuery', action: 'findPatient') {
                apiVersion = 'v2'
            }
            "/patients"(method: 'GET', controller: 'patientQuery', action: 'listPatients') {
                apiVersion = 'v2'
            }
            "/tree_nodes"(method: 'GET', controller: 'tree', action: 'index') {
                apiVersion = 'v2'
            }
            "/files"(method: 'GET', controller: 'storage', action: 'index') {
                apiVersion = "v2"
            }
            "/files/$id"(method: 'GET', controller: 'storage', action: 'show') {
                apiVersion = "v2"
            }
            "/files"(method: 'POST', controller: 'storage', action: 'save') {
                apiVersion = "v2"
            }
            "/files/$id"(method: 'PUT', controller: 'storage', action: 'update') {
                apiVersion = "v2"
            }
            "/files/$id"(method: 'DELETE', controller: 'storage', action: 'delete') {
                apiVersion = "v2"
            }
            "/studies/$studyId/files"(method: 'GET', controller: 'storage', action: 'indexStudy') {
                apiVersion = "v2"
            }
            "/storage"(method: 'GET', controller: 'storageSystem', action: 'index') {
                apiVersion = "v2"
            }
            "/storage/$id"(method: 'GET', controller: 'storageSystem', action: 'show') {
                apiVersion = "v2"
            }
            "/storage"(method: 'POST', controller: 'storageSystem', action: 'save') {
                apiVersion = "v2"
            }
            "/storage/$id"(method: 'DELETE', controller: 'storageSystem', action: 'delete') {
                apiVersion = "v2"
            }
        }

        group "/v1", {

            '/studies'(controller: 'study', method: 'GET', resources: 'study', includes: ['index', 'show'])

            '/studies'(resources: 'study', method: 'GET') {
                '/subjects'(controller: 'subject', resources: 'subject', includes: ['index', 'show'])
            }

            "/studies/$studyId/concepts"(
                    controller: 'concept', action: 'index'
            )

            "/studies/$studyId/concepts/$conceptId**/subjects"(
                    controller: 'subject', action: 'indexByConcept'
            )

            "/studies/$studyId/concepts/$conceptId**/observations"(
                    controller: 'observation', action: 'indexByConcept'
            )

            "/studies/$studyId/concepts/$conceptId**/highdim"(
                    controller: 'highDim', action: 'index', method: 'GET'
            )

            "/studies/$studyId/concepts/$conceptId**/highdim/$dataType"(
                    controller: 'highDim', action: 'download', method: 'GET'
            )

            '/studies'(resources: 'study', method: 'GET') {
                '/observations'(controller: 'observation', resources: 'observation', includes: ['index'])
            }

            '/studies'(resources: 'study', method: 'GET') {
                '/subjects'(resources: 'subject', method: 'GET') {
                    '/observations'(controller: 'observation', action: 'indexBySubject')
                }
            }

            '/patient_sets'(resources: 'patientSet', include: ['index', 'show', 'save'])

            '/observations'(method: 'GET', controller: 'observation', action: 'indexStandalone')
            '/observations2'(method: 'GET', controller: 'observation', action: 'observations2')

            "/studies/$studyId/concepts/$id**"(
                    controller: 'concept', action: 'show', method: 'GET'
            ) {
                constraints {
                    // this mapping has fewer wildcards than .../highdim/<type>
                    // so it will have precedence. Add constraint so it doesn't match
                    id validator: { !(it ==~ '.+/highdim(?:/[^/]+)?') }
                }
            }
        }

    }
}
