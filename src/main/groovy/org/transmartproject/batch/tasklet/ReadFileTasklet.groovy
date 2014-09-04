package org.transmartproject.batch.tasklet

import com.google.common.base.Function
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.InitializingBean

/**
 * Tasklet for reading the contents of a file, given the pathParameter and the reading function
 */
class ReadFileTasklet<T> implements Tasklet, InitializingBean {

    String pathParameter
    Function<File,List<T>> reader

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String path = chunkContext.stepContext.getJobParameters().get(pathParameter)
        if (!path) {
            throw new IllegalArgumentException("No job parameter $pathParameter")
        }

        File file = new File(path)
        List<T> list = reader.apply(file)
        println("read $list")

        return RepeatStatus.FINISHED
    }

    @Override
    void afterPropertiesSet() throws Exception {

    }
}
