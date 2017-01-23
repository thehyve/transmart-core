package org.transmartproject.batch.junit

import groovy.transform.TypeChecked
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

        new SkipIfJobFailedRule(runJobRule: owner.getClass().RUN_JOB_RULE)
    }()

    @Test
    @NoSkipIfJobFailed
    void testJobCompletedSuccessfully() {
        assert skipIfJobFailedRule.jobCompletedSuccessFully,
                'The job completed successfully'
    }

    private Map<String, Object> makeKeyLowerCase(Map<String, Object> map) {
        map.collectEntries { String key, Object value ->
            [ key.toLowerCase(), value ]
        }
    }

    private List<Map<String, Object>> makeKeyLowerCase(List<Map<String, Object>> list) {
        list.collect { Map<String, Object> map -> makeKeyLowerCase(map) }
    }


    List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, paramMap)
        makeKeyLowerCase(result)
    }

    @TypeChecked
    @SuppressWarnings('UnnecessaryPublicModifier')
    List queryForList(String sql, Map<String, ?> paramMap, Class clz) {
        jdbcTemplate.queryForList(sql, paramMap, clz)
    }

    Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) {
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, paramMap)
        makeKeyLowerCase(result)
    }

}
