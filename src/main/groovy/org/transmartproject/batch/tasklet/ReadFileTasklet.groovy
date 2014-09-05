package org.transmartproject.batch.tasklet

import com.google.common.base.Function
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

/**
 * Tasklet for reading the contents of a file, given the pathParameter and the reading function
 */
abstract class ReadFileTasklet<T> implements Tasklet {

    Function<File,List<T>> reader
    File file

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        this.file = getInputFile(chunkContext)
        List<T> list = reader.apply(file)
        println("read $list")
        setResult(chunkContext, list)
        return RepeatStatus.FINISHED
    }

    abstract File getInputFile(ChunkContext ctx)

    abstract void setResult(ChunkContext ctx, List<T> result)

}
