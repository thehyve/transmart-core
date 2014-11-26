package org.transmartproject.batch.highdim.platform

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Beans common to jobs that load annotations.
 */
@Configuration
class PlatformLoadJobConfiguration {

    public static final String BEAN_PLATFORM_OBJECT = 'platformObject'

    @Bean
    @JobScope
    Platform platformObject(@Value('#{jobParameters}') Map<String, Object> parameters) {
        new Platform(
                id: parameters[AbstractPlatformJobParameters.PLATFORM],
                title: parameters[AbstractPlatformJobParameters.TITLE],
                organism: parameters[AbstractPlatformJobParameters.ORGANISM],
                markerType: parameters[AbstractPlatformJobParameters.MARKER_TYPE])
    }
}
