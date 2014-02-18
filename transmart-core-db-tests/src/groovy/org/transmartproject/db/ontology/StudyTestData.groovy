package org.transmartproject.db.ontology

import org.transmartproject.db.i2b2data.I2b2Data

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class StudyTestData {

    I2b2Data i2b2Data = new I2b2Data('STUDY1')

    TableAccess tableAccess = createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
            tableCode: 'i2b2 main', tableName: 'i2b2')

    List<I2b2> i2b2List = {
        [
                createI2b2(level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY1'),
                createI2b2(level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY1'),

                createI2b2(level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY2'),
                createI2b2(level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY2'),
        ]
    }()


    void saveAll() {
        i2b2Data.saveAll()

        org.transmartproject.db.dataquery.highdim.HighDimTestData.save([tableAccess])
        org.transmartproject.db.dataquery.highdim.HighDimTestData.save i2b2List
    }

}
