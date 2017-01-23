package org.transmartproject.batch.startup

import org.junit.Test
import org.transmartproject.batch.clinical.ClinicalDataLoadJobConfiguration
import org.transmartproject.batch.highdim.cnv.data.CnvDataJobConfig
import org.transmartproject.batch.highdim.metabolomics.data.MetabolomicsDataJobConfig
import org.transmartproject.batch.highdim.mirna.data.MirnaDataJobConfig
import org.transmartproject.batch.highdim.mrna.data.MrnaDataJobConfig
import org.transmartproject.batch.highdim.mrna.platform.MrnaPlatformJobConfig
import org.transmartproject.batch.highdim.proteomics.data.ProteomicsDataJobConfig
import org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataJobConfig
import org.transmartproject.batch.tag.TagsLoadJobConfiguration

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests {@link org.transmartproject.batch.startup.RunJob}
 */
class RunJobTests {

    @Test
    void testGetJobStartupDetailsForSingleParamsFile() {
        RunJob runJob = RunJob.createInstance('-p', 'studies/GSE8581/expression.params')

        List<JobStartupDetails> jobsStartupDetails = runJob.jobsStartupDetails

        assertThat jobsStartupDetails, contains(
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
        )
    }

    @Test
    void testGetJobStartupDetailsForMultipleParamsFiles() {
        RunJob runJob = RunJob.createInstance('-p', 'studies/GSE8581/expression.params',
                '-p', 'studies/GSE8581/clinical.params')

        List<JobStartupDetails> jobsStartupDetails = runJob.jobsStartupDetails
        //Note: Despite order of parameters the order in the list has changed with regards to the upload priority
        assertThat jobsStartupDetails, contains(
                hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration)),
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
        )
    }

    @Test
    void testGetJobStartupDetailsForSingleParamsFolder() {
        RunJob runJob = RunJob.createInstance('-f', 'studies/GSE8581/')

        List<JobStartupDetails> jobsStartupDetails = runJob.jobsStartupDetails
        //Note: the order in the list is made with regards to the upload priority
        assertThat jobsStartupDetails, contains(
                hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration)),
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
                hasProperty('jobPath', equalTo(TagsLoadJobConfiguration)),
        )
    }

    @Test
    void testGetJobStartupDetailsForMultipleParamsFoldersAndParamsFiles() {
        RunJob runJob = RunJob.createInstance(
                '-p', 'studies/GPL570_bogus/mrna_annotation.params',
                '-f', 'studies/GSE8581/',
                '-f', 'studies/CLUC/',
                '-p', 'studies/GSE8581/expression.params', // to test duplication reduction
                '-p', 'studies/GSE37427/metabolomics.params',
        )

        List<JobStartupDetails> jobsStartupDetails = runJob.jobsStartupDetails
        //Note: the order in the list is made with regards to the upload priority
        assertThat jobsStartupDetails, contains(
                hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration)),
                hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration)),
                hasProperty('jobPath', equalTo(MrnaPlatformJobConfig)),
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
                hasProperty('jobPath', equalTo(MrnaDataJobConfig)),
                hasProperty('jobPath', equalTo(MetabolomicsDataJobConfig)),
                hasProperty('jobPath', equalTo(ProteomicsDataJobConfig)),
                hasProperty('jobPath', equalTo(RnaSeqDataJobConfig)),
                hasProperty('jobPath', equalTo(CnvDataJobConfig)),
                hasProperty('jobPath', equalTo(MirnaDataJobConfig)),
                hasProperty('jobPath', equalTo(TagsLoadJobConfiguration)),
        )
    }

}
