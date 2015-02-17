package org.transmartproject.batch.support

/**
 *
 */
@Deprecated
interface LineListener {

    void lineRead(String line)

    void lineWritten(String line)

}
