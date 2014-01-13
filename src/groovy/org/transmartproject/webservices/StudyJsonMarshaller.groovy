package org.transmartproject.webservices

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

class StudyJsonMarshaller implements ObjectMarshaller<JSON> {

  public boolean supports(Object object) {
    return object instanceof Study
  }

  public void marshalObject(Object object, JSON converter) {
  	log.error "MARSHALLING!!!!!"
    Study study  = (Study)object
    converter.chars study.name
  }
}