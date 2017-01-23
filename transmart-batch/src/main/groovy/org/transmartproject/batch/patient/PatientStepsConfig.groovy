package org.transmartproject.batch.patient

import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.secureobject.SecureObjectConfig

/**
 * Patient spring configuration
 */
@Configuration
@ComponentScan
@Import([SecureObjectConfig])
class PatientStepsConfig implements StepBuildingConfigurationTrait {

    public static final int LOAD_PATIENTS_CHUNK_SIZE = 512

    @Bean
    Step gatherCurrentPatients(GatherCurrentPatientsReader gatherCurrentPatientsReader, PatientSet patientSet) {
        steps.get('gatherCurrentPatients')
                .chunk(LOAD_PATIENTS_CHUNK_SIZE)
                .reader(gatherCurrentPatientsReader)
                .writer(new PutInBeanWriter(bean: patientSet))
                .allowStartIfComplete(true)
                .build()
    }

}
