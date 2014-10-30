package org.transmartproject.batch.support

/**
 *
 */
interface LineListener {

    void lineRead(String line)

    void lineWritten(String line)

}
