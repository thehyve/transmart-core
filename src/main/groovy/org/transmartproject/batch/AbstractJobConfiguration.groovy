package org.transmartproject.batch

import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

import javax.sql.DataSource

/**
 *
 */
@Import(TransmartAppConfig.class)
@ComponentScan("org.transmartproject.batch")
abstract class AbstractJobConfiguration {

    @Autowired
    JobBuilderFactory jobs

    @Autowired
    StepBuilderFactory steps

    @Value('#{transmartDataSource}')
    DataSource transmartDataSource

    Step stepOf(String name, Tasklet tasklet) {
        steps.get(name)
                .tasklet(tasklet)
                .build()
    }

    Step stepOf(MethodClosure closure) {
        stepOf(closure.method, closure.call())
    }

    Flow flowOf(Step step) {
        new FlowBuilder<SimpleFlow>().start(step).build()
    }

}
