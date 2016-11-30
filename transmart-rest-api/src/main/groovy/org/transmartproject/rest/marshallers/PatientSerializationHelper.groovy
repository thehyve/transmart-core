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

package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.dataquery.Patient

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

class PatientSerializationHelper extends AbstractHalOrJsonSerializationHelper<PatientWrapper> {

    final Class targetType = PatientWrapper

    final String collectionName = 'subjects'

    def convert(PatientWrapper object) {
        getPropertySubsetForSuperType(object.patient, Patient, ['assays'] as Set)
    }

    @Override
    Collection<Link> getLinks(PatientWrapper object) {
        switch(object.apiVersion) {
            case 'v1':
                def studyName = URLEncoder.encode(object.patient.trial.toLowerCase(Locale.ENGLISH), 'utf-8')
                //TODO add more relationships (for instance, the parent study)
                return [new Link(RELATIONSHIP_SELF, "/${object.apiVersion}/studies/$studyName/subjects/${object.patient.id}")]
            default:
                return [new Link(RELATIONSHIP_SELF, "/${object.apiVersion}/patients/${object.patient.id}")]
        }
    }

    @Override
    Map<String, Object> convertToMap(PatientWrapper object) {
        Map<String, Object> result = getPropertySubsetForSuperType(object.patient, Patient, ['assays', 'sex'] as Set)
        result.put('sex', object.patient.sex.name()) //sex has to be manually converted (no support for enums)
        result
    }

}
