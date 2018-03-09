package org.transmartproject.core.config

import groovy.transform.Canonical

import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Canonical
class RuntimeConfigRepresentation implements RuntimeConfig {

    @NotNull
    @Min(1L)
    Integer numberOfWorkers

    @NotNull
    @Min(1L)
    Integer patientSetChunkSize

}
