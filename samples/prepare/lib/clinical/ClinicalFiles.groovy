package lib.clinical

import lib.ParamsFile
import lib.soft.SoftParser
import lib.soft.SoftSampleEntity

class ClinicalFiles {

    private static final String DATA_TYPE = 'clinical'

    String studyId

    SoftParser parser

    private ClinicalData clinicalData

    void writeFiles() {
        File studyDir = new File(studyId)
        File dataDir = new File(studyDir, DATA_TYPE)
        dataDir.exists() || dataDir.mkdirs()

        prepareClinicalData dataDir
        clinicalData.writeFiles()

        ParamsFile paramsFile = getParamsFile(new File(studyDir, "${DATA_TYPE}.params"))
        paramsFile.write()
    }

    private void prepareClinicalData(File targetDir) {
        clinicalData = new ClinicalData(
                targetDir: targetDir,
                filePrefix: studyId
        )

        parser.getEntitiesOfType(SoftSampleEntity).each {
            processSample it
        }
    }

    private void processSample(SoftSampleEntity sampleEntity) {
        String subjectId = sampleEntity.name

        Map<Column, Object> data = sampleEntity['Sample_characteristics_ch1*'].collectEntries {
            def split = it.split(': ')
            String characteristicName = split[0]
            String characteristicValue = split[1]

            [ getColumnForCharacteristic(characteristicName),
                   characteristicValue ]
        }

        clinicalData.addPatient subjectId, data
    }

    Column getColumnForCharacteristic(String name) {
        if (name == 'gender') {
            return new Column(
                    categoryCode: 'Subjects',
                    dataLabel: Column.SEX_LABEL
            )
        }
        new Column(
                categoryCode: 'Characteristics',
                dataLabel: name
        )
    }

    private ParamsFile getParamsFile(File targetFile) {
        new ParamsFile(
                file: targetFile,
                params: [
                        COLUMN_MAP_FILE:       clinicalData.columnsFile.name,
                        WORD_MAP_FILE:         clinicalData.wordMapFile.name,
                        RECORD_EXCLUSION_FILE: 'x',
                ]
        )
    }
}
