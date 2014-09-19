package org.transmartproject.batch.support

import org.junit.Assert
import org.junit.Test

/**
 *
 */
class ModelHelperTest {

    @Test
    void testGetJoinedConceptPath() {
        String value = ModelHelper.getJoinedConceptPath('\\foo\\bar\\', 'aaa+bbb', 'bb+cc', 'ddd')
        Assert.assertEquals('\\foo\\bar\\aaa\\bbb\\bb\\cc\\ddd', value)
    }
}
