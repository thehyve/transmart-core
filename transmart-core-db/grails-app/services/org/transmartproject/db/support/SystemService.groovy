package org.transmartproject.db.support

import groovy.transform.CompileStatic
import org.modelmapper.ModelMapper
import org.transmartproject.core.config.RuntimeConfig
import org.transmartproject.core.config.SystemResource
import org.transmartproject.db.config.RuntimeConfigImpl
import org.transmartproject.core.config.RuntimeConfigRepresentation

import javax.validation.Valid

@CompileStatic
class SystemService implements SystemResource {

    private final int DEFAULT_PATIENT_SET_CHUNK_SIZE = 10000

    private final RuntimeConfigImpl runtimeConfig = new RuntimeConfigImpl(
            Runtime.getRuntime().availableProcessors(),
            DEFAULT_PATIENT_SET_CHUNK_SIZE
    )

    private final ModelMapper modelMapper = new ModelMapper()

    RuntimeConfig getRuntimeConfig() {
        return modelMapper.map(runtimeConfig, RuntimeConfigRepresentation.class)
    }

    RuntimeConfig updateRuntimeConfig(@Valid RuntimeConfig config) {
        runtimeConfig.setNumberOfWorkers(config.numberOfWorkers)
        runtimeConfig.setPatientSetChunkSize(config.patientSetChunkSize)
        getRuntimeConfig()
    }

}
