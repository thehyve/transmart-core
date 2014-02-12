package org.transmartproject.db.i2b2data

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'fullName,trial')
class I2b2TrialNodes implements Serializable {

    // is a view!

    String fullName
    String trial

    static mapping = {
        table    name:      'i2b2_trial_nodes', schema: 'i2b2metadata'
        id       composite: ['fullName', 'trial']

        fullName column: 'c_fullname'

        version  false

        /* I don't think it's possible to create an association to I2b2 here
         * For some reason, the id of I2b2 is [C_FULLNAME, C_NAME].
         * Maybe we could change the primary key there to be just C_FULL_NAME,
         * (I don't see why not) but it would need more investigation */
    }
}
