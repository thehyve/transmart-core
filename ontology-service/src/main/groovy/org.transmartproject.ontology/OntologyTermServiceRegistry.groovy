/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.ontology

import groovy.util.logging.Slf4j

@Singleton(strict=false)
@Slf4j
class OntologyTermServiceRegistry {

    public final defaultServiceType = DefaultExternalOntologyTermService.name

    def serviceTypes = [:] as Map<String, Class>

    void register(String name, Class<ExternalOntologyTermService> serviceType) {
        log.info "Registering ontology service with name '${name}'."
        if (serviceTypes.containsKey(name)) {
            log.warn "Ontology service with name '${name}' already registered. Skip registration."
            return
        }
        serviceTypes[name] = serviceType
    }

    void init() {
        register(DefaultExternalOntologyTermService.name, DefaultExternalOntologyTermService)
        register(BioOntologyApiOntologyTermService.name, BioOntologyApiOntologyTermService)
    }

    private OntologyTermServiceRegistry() {
        init()
    }

    ExternalOntologyTermService create(String type, Map parameters) {
        def serviceType = serviceTypes[type]
        if (serviceType == null) {
            throw new OntologyTermServiceTypeNotFound("Ontology service of type '${type}' not found.")
        }
        def service = serviceType.newInstance() as ExternalOntologyTermService
        service.init(parameters)
        service
    }

}
