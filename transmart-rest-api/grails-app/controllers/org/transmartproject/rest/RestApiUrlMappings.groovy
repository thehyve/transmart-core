package org.transmartproject.rest
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

import grails.util.Environment

class RestApiUrlMappings {

    // grails url-mappings-report can come handy here...

    static mappings = {

        '/businessException/index'(controller: 'businessException', action: 'index')
        '/versions'(method: 'GET', controller: 'version', action: 'index')
        "/versions/$id"(method: 'GET', controller: 'version', action: 'show')

        group "/v2", {
            "/versions"(method: 'GET', controller: 'version', action: 'index')
            "/versions/$id"(method: 'GET', controller: 'version', action: 'show')

            "/studies"(method: 'GET', controller: 'studyQuery', action: 'listStudies') {
                apiVersion = 'v2'
            }
            "/studies/$id"(method: 'GET', controller: 'studyQuery', action: 'findStudy') {
                apiVersion = 'v2'
            }
            "/studies/studyIds"(method: 'GET', controller: 'studyQuery', action: 'findStudiesByStudyIds') {
                apiVersion = 'v2'
            }
            "/studies/studyId/$studyId"(method: 'GET', controller: 'studyQuery', action: 'findStudyByStudyId') {
                apiVersion = 'v2'
            }
            "/observations"(controller: 'query') {
                action = [GET: 'observations', POST: 'observations']
                apiVersion = 'v2'
            }
            "/observations/table"(controller: 'query') {
                action = [POST: 'table']
                apiVersion = 'v2'
            }
            "/observations/crosstable"(controller: 'query') {
                action = [POST: 'crosstable']
                apiVersion = 'v2'
            }
            "/supported_fields"(method: 'GET', controller: 'query', action: 'supportedFields') {
                apiVersion = 'v2'
            }
            "/observations/aggregates_per_concept"(controller: 'query') {
                action = [GET: 'aggregatesPerConcept', POST: 'aggregatesPerConcept']
                apiVersion = 'v2'
            }
            "/observations/counts"(controller: 'query') {
                action = [GET: 'counts', POST: 'counts']
                apiVersion = 'v2'
            }
            "/observations/counts_per_concept"(controller: 'query') {
                action = [GET: 'countsPerConcept', POST: 'countsPerConcept']
                apiVersion = 'v2'
            }
            "/observations/counts_per_study"(controller: 'query') {
                action = [GET: 'countsPerStudy', POST: 'countsPerStudy']
                apiVersion = 'v2'
            }
            "/observations/counts_per_study_and_concept"(controller: 'query') {
                action = [GET: 'countsPerStudyAndConcept', POST: 'countsPerStudyAndConcept']
                apiVersion = 'v2'
            }
            "/patient_sets/$id"(method: 'GET', controller: 'patientQuery', action: 'findPatientSet') {
                apiVersion = 'v2'
            }
            "/patient_sets"(method: 'GET', controller: 'patientQuery', action: 'findPatientSets') {
                apiVersion = 'v2'
            }
            "/patient_sets"(method: 'POST', controller: 'patientQuery', action: 'createPatientSet') {
                apiVersion = 'v2'
            }
            "/patients/$id"(controller: 'patientQuery') {
                action = [GET: 'findPatient', POST: 'findPatient']
                apiVersion = 'v2'
            }
            "/patients"(controller: 'patientQuery') {
                action = [GET: 'listPatients', POST: 'listPatients']
                apiVersion = 'v2'
            }
            "/concepts"(method: 'GET', controller: 'concept', action: 'index') {
                apiVersion = 'v2'
            }
            "/concepts/${conceptCode}"(method: 'GET', controller: 'concept', action: 'show') {
                apiVersion = 'v2'
            }
            "/tree_nodes"(method: 'GET', controller: 'tree', action: 'index') {
                apiVersion = 'v2'
            }
            "/tree_nodes/clear_cache"(method: 'GET', controller: 'tree', action: 'clearCache') {
                apiVersion = 'v2'
            }
            "/system/after_data_loading_update"(method: 'GET', controller: 'system', action: 'afterDataLoadingUpdate') {
                apiVersion = 'v2'
            }
            "/tree_nodes/rebuild_cache"(method: 'GET', controller: 'tree', action: 'rebuildCache') {
                apiVersion = 'v2'
            }
            "/tree_nodes/rebuild_status"(method: 'GET', controller: 'tree', action: 'rebuildStatus') {
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
            "/storage/$id"(method: 'PUT', controller: 'storageSystem', action: 'update') {
                apiVersion = "v2"
            }
            "/arvados/workflows"(method: 'GET', controller: 'arvados', action: 'index') {
                apiVersion = "v2"
            }
            "/arvados/workflows/$id"(method: 'GET', controller: 'arvados', action: 'show') {
                apiVersion = "v2"
            }
            "/arvados/workflows"(method: 'POST', controller: 'arvados', action: 'save') {
                apiVersion = "v2"
            }
            "/arvados/workflows/$id"(method: 'DELETE', controller: 'arvados', action: 'delete') {
                apiVersion = "v2"
            }
            "/arvados/workflows/$id"(method: 'PUT', controller: 'arvados', action: 'update') {
                apiVersion = "v2"
            }
            "/dimensions/$dimensionName/elements"(controller: 'dimension') {
                action = [GET: 'list', POST: 'list']
                apiVersion = 'v2'
            }
            "/export/job"(method: 'POST', controller: 'export', action: 'createJob') {
                apiVersion = "v2"
            }
            "/export/$jobId/run"(method: 'POST', controller: 'export', action: 'run') {
                apiVersion = "v2"
            }
            "/export/$jobId/cancel"(method: 'POST', controller: 'export', action: 'cancel') {
                apiVersion = "v2"
            }
            "/export/$jobId"(method: 'DELETE', controller: 'export', action: 'delete') {
                apiVersion = "v2"
            }
            "/export/$jobId"(method: 'GET', controller: 'export', action: 'get') {
                apiVersion = "v2"
            }
            "/export/$jobId/download"(method: 'GET', controller: 'export', action: 'download') {
                apiVersion = "v2"
            }
            "/export/$jobId/status"(method: 'GET', controller: 'export', action: 'jobStatus') {
                apiVersion = "v2"
            }
            "/export/jobs"(method: 'GET', controller: 'export', action: 'listJobs') {
                apiVersion = "v2"
            }
            "/export/data_formats"(method: 'POST', controller: 'export', action: 'dataFormats') {
                apiVersion = "v2"
            }
            "/export/file_formats"(method: 'GET', controller: 'export', action: 'fileFormats') {
                apiVersion = "v2"
            }
            "/queries"(method: 'GET', controller: 'userQuery', action: 'index') {
                apiVersion = "v2"
            }
            "/queries/$id"(method: 'GET', controller: 'userQuery', action: 'get') {
                apiVersion = "v2"
            }
            "/queries"(method: 'POST', controller: 'userQuery', action: 'save') {
                apiVersion = "v2"
            }
            "/queries/$id"(method: 'PUT', controller: 'userQuery', action: 'update') {
                apiVersion = "v2"
            }
            "/queries/$id"(method: 'DELETE', controller: 'userQuery', action: 'delete')
            "/queries/sets/scan"(method: 'POST', controller: 'userQuerySet', action: 'scan')
            "/queries/$queryId/sets"(method: 'GET', controller: 'userQuerySet', action: 'getSetChangesByQueryId')
            "/pedigree/relation_types"(method: 'GET', controller: 'relationType', action: 'index')
            "/config"(controller: 'config') {
                action = [GET: 'index', PUT: 'update']
            }
        }

        group "/v1", {
            "/versions"(method: 'GET', controller: 'version', action: 'index')
            "/versions/$id"(method: 'GET', controller: 'version', action: 'show')

            '/studies'(controller: 'study', method: 'GET', resources: 'study', includes: ['index', 'show'])

            '/studies'(resources: 'study', method: 'GET') {
                '/subjects'(controller: 'subject', resources: 'subject', includes: ['index', 'show'])
            }

            "/studies/$studyId/concepts"(
                    controller: 'ontologyTerm', action: 'index'
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
                    controller: 'ontologyTerm', action: 'show', method: 'GET'
            ) {
                constraints {
                    // this mapping has fewer wildcards than .../highdim/<type>
                    // so it will have precedence. Add constraint so it doesn't match
                    id validator: { !(it ==~ '.+/highdim(?:/[^/]+)?') }
                }
            }
        }

        if (Environment.current.name == 'test') {
            '/test/createData'(controller: 'test', action: 'createData')
        }
    }
}
