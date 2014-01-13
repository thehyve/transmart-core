package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.webservices.Study
import org.transmartproject.webservices.Observation
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_USER'])
class StudyController {

    /** GET request on /studies/
     *  This will return the list of studies, where each study will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
        println "oauth setting:${grailsApplication.config.grails.plugin.springsecurity.oauthProvider.active}"
        Study study1 = new Study(name:"yeah")
        Study study2 = new Study(name:"ohno")
        Study study3 = new Study(name:"ole!")
        List list = [study1,study2,study3]
        log.info "json:${study1 as JSON}"
        render list as JSON
    }

    /** GET request on /studies/${id}
     *  This returns the single requested entity.
     *
     *  @param id The is for which to return study information.
     */
    def show(Integer id) {
        log.info "in show method for study with id:$id"
        Study study = new Study(name:"yeah", id:2)
        render study as JSON
    }

    /**
     *    GET /studies  index
     *    GET /studies/create   create
     *    POST    /studies  save
     *    GET /studies/${id}    show
     *    GET /studies/${id}/edit   edit
     *    PUT /studies/${id}    update
     *    DELETE  /studies/${id}    delete
    **/
}
