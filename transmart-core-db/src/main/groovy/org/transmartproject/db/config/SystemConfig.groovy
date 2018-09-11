package org.transmartproject.db.config

import grails.util.Holders
import groovy.util.logging.Slf4j
import jsr166y.ForkJoinPool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
class SystemConfig {

    @Bean
    ForkJoinPool workerPool() {
        def numberOfWorkers = Holders.config.getProperty(
                'org.transmartproject.system.numberOfWorkers',
                Integer.class,
                Runtime.getRuntime().availableProcessors())
        log.info "Create new worker pool for ${numberOfWorkers} workers"
        new ForkJoinPool(numberOfWorkers)
    }

}
