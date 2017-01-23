package org.transmartproject.batch.junit

import org.junit.rules.ExternalResource
import org.springframework.batch.core.*
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.io.Resource
import org.transmartproject.batch.db.TableTruncator

import javax.batch.operations.BatchRuntimeException

/**
 * JUnit rule to load a db tables' content from tsv files and truncate the
 * table afterwards.
 */
class LoadTablesRule extends ExternalResource {

    private final Map<String /* table */, Resource /* file */> tableFileMap

    @Autowired
    private JobLauncher jobLauncher

    @Autowired
    private Job job

    @Autowired
    private TableTruncator tableTruncator

    LoadTablesRule(Map<String, Resource> tableFileMap) {
        this.tableFileMap = tableFileMap
    }

    protected void before() throws Throwable {
        def appCtx = new GenericApplicationContext()
        AnnotationConfigUtils.registerAnnotationConfigProcessors(appCtx)
        appCtx.registerBeanDefinition('loadTablesConfiguration',
                new GenericBeanDefinition(
                        beanClass: LoadTablesConfiguration,
                        propertyValues: new MutablePropertyValues(
                                tableFileMap: tableFileMap)))
        appCtx.refresh()

        appCtx.autowireCapableBeanFactory.autowireBean(this)

        // include identifying parameter with date to force restart
        JobExecution execution = jobLauncher.run(job, new JobParameters(
                date: new JobParameter(new Date(), true)))
        if (execution.exitStatus.exitCode != ExitStatus.COMPLETED.exitCode) {
            throw new BatchRuntimeException("Failed loading data for " +
                    "tables ${tableFileMap.keySet()}, exit status was " +
                    "$execution.exitStatus, exceptions " +
                    "$execution.allFailureExceptions")
        }
    }

    protected void after() {
        tableTruncator.truncate(tableFileMap.keySet())
    }
}
