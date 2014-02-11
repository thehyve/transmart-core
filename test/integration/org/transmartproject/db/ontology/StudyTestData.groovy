package org.transmartproject.db.ontology

import org.transmartproject.db.i2b2data.I2b2Data

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class StudyTestData {

    I2b2Data i2b2Data = new I2b2Data('study1')

    TableAccess tableAccess = createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
            tableCode: 'i2b2 main', tableName: 'i2b2')

    List<I2b2> i2b2List = {
        [
                createI2b2(level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cVisualattributes: 'FAS'),
                createI2b2(level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cVisualattributes: 'LA'),

                createI2b2(level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cVisualattributes: 'FAS'),
                createI2b2(level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cVisualattributes: 'LA'),
        ]
    }()


    void saveAll() {
        i2b2Data.saveAll()

        save([tableAccess])
        save i2b2List
    }

}
