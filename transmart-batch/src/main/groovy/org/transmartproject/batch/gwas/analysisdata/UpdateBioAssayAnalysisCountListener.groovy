package org.transmartproject.batch.gwas.analysisdata

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.listener.StepExecutionListenerSupport
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.transmartproject.batch.biodata.BioAssayAnalysisDAO
import org.transmartproject.batch.gwas.metadata.CurrentGwasAnalysisContext

/**
 * Updates the data_count column of biomart.bio_assay_analysis based on the
 * write count of the step.
 */
@Slf4j
@Component
class UpdateBioAssayAnalysisCountListener extends StepExecutionListenerSupport implements BeanNameAware {

    String beanName

    @Autowired
    private CurrentGwasAnalysisContext currentGwasAnalysisContext

    @Autowired
    private ApplicationContext applicationContext

    @Autowired
    private BioAssayAnalysisDAO bioAssayAnalysisDAO

    @SuppressWarnings('CatchException')
    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Step status was ${stepExecution.exitStatus}; " +
                    "will not write ")
            return stepExecution.exitStatus
        }

        try {
            ((UpdateBioAssayAnalysisCountListener)
                    applicationContext.getBean(beanName)).insertWithinTransaction(
                    currentGwasAnalysisContext.bioAssayAnalysisId,
                    currentGwasAnalysisContext.analysisRowCount)

            stepExecution.exitStatus
        } catch (Exception e) {
            log.error('Error setting count for bio_assay_analysis_id ' +
                    currentGwasAnalysisContext.bioAssayAnalysisId, e)
            ExitStatus.FAILED
        }
    }

    // if it fails, it will throw an unchecked exception, thereby rolling
    // back the transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void insertWithinTransaction(long bioAssayAnalysisId, long count) {
        bioAssayAnalysisDAO.updateBioAssayAnalysisCount(
                bioAssayAnalysisId,
                count)

        log.info("Set count for bio_assay_analysis $bioAssayAnalysisId " +
                "at $count")
    }


}
