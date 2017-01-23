package org.transmartproject.batch.i2b2.dimensions

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.i2b2.misc.I2b2ControlColumnsHelper

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY

/**
 * Writes visits (with no data) to visit_dimension and encounter_mapping.
 */
@Component
@JobScopeInterfaced
class InsertVisitsWriter implements ItemWriter<DimensionsStoreEntry> {

    @Value('#{tables.visitDimension}')
    private SimpleJdbcInsert visitDimensionInsert

    @Value('#{tables.encounterMapping}')
    private SimpleJdbcInsert encounterMappingInsert

    @Value("#{jobParameters['VISIT_IDE_SOURCE']}")
    private String visitIdeSource

    @Value("#{jobParameters['PATIENT_IDE_SOURCE']}")
    private String patientIdeSource

    @Value("#{jobParameters['PROJECT_ID']}")
    private String projectId

    @Autowired
    private I2b2ControlColumnsHelper i2b2ControlColumnsHelper

    @Autowired
    private DimensionsStore dimensionsStore

    @Override
    void write(List<? extends DimensionsStoreEntry> items) throws Exception {
        int[] res

        res = visitDimensionInsert.executeBatch(
                items.collect {
                    assert it.extraData != null

                    Object patientNum = dimensionsStore.
                            getInternalIdFor PATIENT_DIMENSION_KEY, it.extraData
                    assert patientNum != null
                    patientNum = patientNum as Long

                    [
                            encounter_num: it.internalId as Long,
                            patient_num  : patientNum,
                            *            : i2b2ControlColumnsHelper.controlValues,
                    ]
                } as Map[])

        DatabaseUtil.checkUpdateCounts(res,
                "inserting into $visitDimensionInsert.tableName")

        res = encounterMappingInsert.executeBatch(
                items.collect {
                    [
                            project_id          : projectId,
                            encounter_ide       : it.externalId,
                            encounter_ide_source: visitIdeSource,
                            encounter_num       : it.internalId as Long,
                            patient_ide         : it.extraData,
                            patient_ide_source  : patientIdeSource,
                            *                   : i2b2ControlColumnsHelper.controlValues,
                    ]
                } as Map[])
        DatabaseUtil.checkUpdateCounts(res,
                "inserting into $encounterMappingInsert.tableName")
    }
}
