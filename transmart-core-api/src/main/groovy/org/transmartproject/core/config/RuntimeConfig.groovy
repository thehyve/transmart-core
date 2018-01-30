package org.transmartproject.core.config

interface RuntimeConfig {

    /**
     * Maximum number of workers that are spawned in parallel for a parallelisable task.
     */
    Integer getNumberOfWorkers()

    /**
     * Chunk size for splitting patient set based tasks into smaller subtasks.
     */
    Integer getPatientSetChunkSize()

}
