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

package org.transmartproject.rest

import grails.rest.Link
import grails.rest.render.util.AbstractLinkingRenderer

import javax.annotation.Resource

import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.db.ontology.StudyAccessImpl

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.API_READ
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.EXPORT

class StudyController {

    static responseFormats = ['json', 'hal']

    @Resource
    StudiesResource studiesResourceService

    /** GET request on /v1/studies/
     *  This will return the list of studies, where each study will be rendered in its short format
    */
    def index() {
        def studiesAccess = []
        def studies = studiesResourceService.studySet
        //Checks to which studies the user has access.
        studies.each { study ->
            boolean accessView = currentUser.canPerform(API_READ, study)
            boolean accessExport = currentUser.canPerform(EXPORT, study)
            //Possibility of adding more access types.
            Map accessibleByUser = [
                    view:accessView,
                    export:accessExport]
            StudyAccessImpl studyAccessImpl= new StudyAccessImpl(
                    accessibleByUser:accessibleByUser,
                    study:study)
            studiesAccess.add(studyAccessImpl)
        }
        def studiesAccessWrapped =  wrapStudies(studiesAccess)
        respond studiesAccessWrapped
    }

    /** GET request on /v1/studies/${id}
     *  This returns the single study by name.
     *
     *  @param name the name of the study
     */
    def show(String id) {
        def studyImpl =  studiesResourceService.getStudyById(id)
        //Check if the user has access to the specific study.
        boolean accessView = currentUser.canPerform(API_READ, studyImpl)
        boolean accessExport = currentUser.canPerform(EXPORT, studyImpl)
        //Possibility of adding more access types.
        Map accessibleByUser = [
                view:accessView,
                export:accessExport]
        StudyAccessImpl studyAccessImpl = new StudyAccessImpl(
                accessibleByUser:accessibleByUser,
                study:studyImpl,
                )
        respond studyAccessImpl
    }
    
    private wrapStudies(Object source) {
        new ContainerResponseWrapper
        (
            container: source,
            componentType: Study,
            links: [ new Link(AbstractLinkingRenderer.RELATIONSHIP_SELF, "/v1/studies") ]
        )
    }
    
}
