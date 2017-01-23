package org.transmartproject.batch.secureobject

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.biodata.BioDataConfig

/**
 * Transmart security objects spring configuration
 */
@Configuration
@ComponentScan
@Import(BioDataConfig)
class SecureObjectConfig {}
