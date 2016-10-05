package org.transmartproject.db.i2b2data

class Study {

    long id

    String name

    static constraints = {
    }
    static mapping = {
        table schema: 'I2B2DEMODATA'
        id    generator: 'assigned', column: 'id', type: Long

    }
}
