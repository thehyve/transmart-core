package org.transmartproject.batch.junit

import org.junit.Rule
import org.junit.Test
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.db.RowCounter

import static java.lang.reflect.Modifier.isStatic

/**
 * Common stuff for tests that launch jobs.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
trait JobRunningTestTrait {

    @Autowired
    JobRepository jobRepository

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final SkipIfJobFailedRule skipIfJobFailedRule = {
        if (!owner.getClass().declaredFields.find {
            it.name == 'RUN_JOB_RULE' && isStatic(it.modifiers)
        }) {
            throw new NoSuchFieldException("Expected the class " +
                    "${owner.getClass()} to have a static property 'RUN_JOB_RULE'")
        }

        new SkipIfJobFailedRule(
                jobRepositoryProvider: { -> jobRepository },
                runJobRule: owner.getClass().RUN_JOB_RULE)
    }()

    @Test
    @NoSkipIfJobFailed
    void testJobCompletedSuccessfully() {
        assert skipIfJobFailedRule.jobCompletedSuccessFully,
                'The job completed successfully'
    }

}
