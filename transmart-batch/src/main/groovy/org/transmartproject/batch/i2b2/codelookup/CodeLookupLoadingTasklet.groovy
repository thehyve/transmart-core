package org.transmartproject.batch.i2b2.codelookup

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.UnexpectedJobExecutionException
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.Assert
import org.transmartproject.batch.i2b2.database.I2b2Tables
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable

import javax.annotation.PostConstruct

import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.ENUMERATION_LOOKUP

/**
 * Loads all the codes into memory.
 */
@Slf4j
class CodeLookupLoadingTasklet implements Tasklet {

    Set<DimensionI2b2Variable> dimensionVariables

    @Autowired
    CodeLookupStore codeLookupStore

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private I2b2Tables tables

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Iterator<DimensionI2b2Variable> variableIterator =
            dimensionVariables.iterator()

    @PostConstruct
    void init() {
        Assert.notNull(dimensionVariables, 'dimensionVariables not set')
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!variableIterator.hasNext()) {
            log.debug('End of dimension variables iterator')
            return RepeatStatus.FINISHED
        }

        DimensionI2b2Variable var = variableIterator.next()
        if (var.variableType != ENUMERATION_LOOKUP) {
            log.debug("Variable $var not of type ENUMERATION_LOOKUP; skipping")
            return RepeatStatus.CONTINUABLE
        }

        def table = var.dimensionTable.toUpperCase(Locale.ENGLISH)
        assert !table.contains('.')
        def column = var.dimensionColumn.toUpperCase(Locale.ENGLISH)

        def res = jdbcTemplate.queryForList """
                SELECT code_cd FROM $tables.codeLookup
                WHERE table_cd = :table AND column_cd = :column""",
                [table: table, column: column], String

        if (res.size() == 0) {
            log.error("Found no codes in i2b2demodata.code_lookup for $var (" +
                    "table = $table, column = " +
                    "$var.dimensionColumn). Fill the enumeration values in " +
                    "code_lookup first.")
            throw new UnexpectedJobExecutionException(
                    "No enumeration values for $var")
        }

        log.info("Found ${res.size()} codes for $var")

        res.each { code ->
            log.debug("Found code $code for " +
                    "$var.dimensionTable/$var.dimensionColumn")
            codeLookupStore.add var.dimensionTable, var.dimensionColumn, code
            contribution.incrementReadCount()
        }

        RepeatStatus.CONTINUABLE
    }
}
