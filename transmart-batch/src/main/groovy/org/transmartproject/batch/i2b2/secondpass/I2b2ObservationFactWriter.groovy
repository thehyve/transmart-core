package org.transmartproject.batch.i2b2.secondpass

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.i2b2.fact.FactGroup
import org.transmartproject.batch.i2b2.fact.FactValue
import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry
import org.transmartproject.batch.i2b2.misc.I2b2ControlColumnsHelper
import org.transmartproject.batch.i2b2.variable.ModifierI2b2Variable

/**
 * Writes the facts on a {@link I2b2SecondPassRow} to observation_fact.
 */
@Component
@JobScope
@Slf4j
class I2b2ObservationFactWriter implements ItemWriter<I2b2SecondPassRow> {

    @Autowired
    private I2b2ControlColumnsHelper controlColumnsHelper

    @Value('#{tables.observationFact}')
    private SimpleJdbcInsert inserter

    private boolean configured

    @Override
    void write(List<? extends I2b2SecondPassRow> items) throws Exception {
        log.trace("Inserting ${items.size()} rows")

        List<Map<String, Object>> rows = []
        items.each { row ->
            row.factGroups.each { factGroup ->
                rows << factToRow(row, factGroup, factGroup.conceptFact)
                factGroup.modifierFacts.each { I2b2MappingEntry modifierEntry,
                                               FactValue factValue ->
                    rows << factToRow(row, factGroup, factValue,
                            ((ModifierI2b2Variable) modifierEntry.i2b2Variable)
                                    .modifierCode)
                }
            }
        }

        // make sure it doesn't include not null columns with default values
        // that we're not specifying
        if (!configured) {
            inserter.usingColumns(*(rows.first().keySet() as List))
            configured = true
        }
        int[] counts = inserter.executeBatch(rows as Map[])
        DatabaseUtil.checkUpdateCounts(counts,
                "inserting in $inserter.tableName")
    }

    private Map<String, Object> factToRow(I2b2SecondPassRow row,
                                          FactGroup factGroup,
                                          FactValue fact,
                                          String modifierCd = '@') {
        [
                encounter_num   : row.encounterNum,
                patient_num     : row.patientNum,
                concept_cd      : factGroup.conceptCode,
                provider_id     : row.providerId,
                start_date      : row.startDate,
                modifier_cd     : modifierCd,
                instance_num    : factGroup.instanceNum,
                valtype_cd      : fact.dataType.valueTypeCode,
                tval_char       : fact.textValue,
                nval_num        : fact.numberValue,
                valueflag_cd    : fact.valueFlag,
                units_cd        : factGroup.conceptEntry.unit,
                end_date        : row.endDate,
                observation_blob: fact.blob,
                *               : controlColumnsHelper.controlValues,
        ]
    }
}
