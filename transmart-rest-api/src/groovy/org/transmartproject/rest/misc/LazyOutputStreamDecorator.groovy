/*
 * Copyright 2014-2015 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.misc

import groovy.transform.CompileStatic

/**
 * Retrieve an OutputStream at last minute to avoid exceptions thrown
 * after the OutputStream having been requested but before it has been
 * written to being masked by "getOutputStream() has already been called
 * for this response"
 */
@CompileStatic
class LazyOutputStreamDecorator extends OutputStream {

    Closure<OutputStream> outputStreamProducer

    private OutputStream delegate

    void ensureOpened() {
        if (!delegate) {
            delegate = outputStreamProducer.call()
        }
    }

    @Override
    void write(int b) throws IOException {
        ensureOpened()
        delegate.write(b)
    }

    @Override
    void write(byte[] b) throws IOException {
        ensureOpened()
        delegate.write(b)
    }

    @Override
    void write(byte[] b, int off, int len) throws IOException {
        ensureOpened()
        delegate.write(b, off, len)
    }

    @Override
    void flush() throws IOException {
        ensureOpened()
        delegate.flush()
    }

    @Override
    void close() throws IOException {
        if (delegate) {
            delegate.close()
        }
    }
}
