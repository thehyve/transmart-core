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

package org.transmartproject.rest.test

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.ontology.StudyAccess
import org.transmartproject.rest.StudyLoadingService

class StubStudyLoadingService extends StudyLoadingService {

    Study storedStudy

    @Override
    Study fetchStudy() {
        storedStudy
    }

    static Study createStudy(String studyId, String key) {
        def study
        study = [
                getId: { -> studyId },
                getOntologyTerm: { ->
                    [
                            getName:     { -> getComponents(key, -1) },
                            getFullName: { -> '\\' + getComponents(key, 3, -1) + '\\' },
                            getKey: { -> key },
                            getVisualAttributes: { -> EnumSet.of(VisualAttributes.STUDY)},
                            getStudy: { -> study },
                            getChildren: { -> [] },
                            getLevel: { -> 0 }, //just to make sure we have no parent
                            //getLevel: { -> key.split('\\\\').length },
                            getPatientCount: { -> 0}
                    ] as OntologyTerm
                }
        ] as Study
    }

    static StudyAccess createStudyAccess(String studyId, String key, Map accessibleByUser) {
        def studyAccess
        studyAccess = [
                getStudy: { ->
                    [
                    getId          : { -> studyId },
                    getOntologyTerm: { ->
                        [
                                getName            : { -> getComponents(key, -1) },
                                getFullName        : { -> '\\' + getComponents(key, 3, -1) + '\\' },
                                getKey             : { -> key },
                                getVisualAttributes: { -> EnumSet.of(VisualAttributes.STUDY) },
                                getStudy           : { -> createStudy(studyId, key) },
                                getChildren        : { -> [] },
                                getLevel           : { -> 0 }, //just to make sure we have no parent
                                //getLevel: { -> key.split('\\\\').length },
                                getPatientCount    : { -> 0 }
                        ] as OntologyTerm
                    },
                    ] as Study
                },
                getAccessibleByUser: { -> accessibleByUser
                }
        ] as StudyAccess
        studyAccess
    }

    private static String getComponents(String key, int a, int b = a) {
        (key.split('\\\\') as List)[a..b].join('\\')
    }
}
