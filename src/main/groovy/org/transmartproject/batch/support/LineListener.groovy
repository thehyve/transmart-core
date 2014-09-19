package org.transmartproject.batch.support

/**
 *
 */
public interface LineListener {

    void lineRead(String line)

    void lineWritten(String line)

}