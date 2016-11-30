/*
 * Copyright © 2013-2015 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.snp_lz;

import com.google.common.base.Charsets;
import com.google.common.base.Function
import groovy.transform.CompileStatic;

import java.sql.Blob;
import java.util.zip.GZIPInputStream;

/**
 * Tokenizes a Blob field.
 */
@CompileStatic
class GzipFieldTokenizer {

    private Blob blob
    private int expectedSize

    // Groovy can do int operations unboxed, but not char operations
    static final int space = (' ' as char) as int

    public GzipFieldTokenizer(Blob blob, int expectedSize) {
        this.blob = blob
        this.expectedSize = expectedSize
    }

    private void withTokens(Function<String, Object> action) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(blob.getBinaryStream()), Charsets.US_ASCII));

        try {
            StringBuilder builder = new StringBuilder();
            int c, size = 0
            while ((c = reader.read()) >= 0) {
                if (c == space) {
                    size++
                    if (size > expectedSize - 1) {
                        throw new InputMismatchException("Got more tokens than the $expectedSize expected")
                    }
                    action.apply(builder.toString())
                    builder.setLength(0)
                } else {
                    // On OpenJDK 1.8 this `as` conversion is as fast as doing `(char) c`, so the JVM is able to inline
                    // the conversion overhead.
                    builder.append(c as char)
                }
            }

            if (size > 0 || builder.size()) size += 1
            // check this first to make sure we don't call action once too much
            if (size != expectedSize) {
                throw new InputMismatchException("Expected $expectedSize tokens, but got only $size")
            }
            if (size) {
                action.apply(builder.toString())
            }
        } finally {
            reader.close()
        }
    }

    public double[] asDoubleArray() {
        double[] res = new double[expectedSize]
        withTokens(new Function<String,Object>() {
            // Having these as inner object fields avoids an indirection through Reference in the inner loop
            double[] arr = res
            int i = 0
            Object apply(String tok) {
                arr[i++] = Double.parseDouble(tok)
            }
        })
        return res
    }

    /**
     * @throws InputMismatchException iff the number of values read &ne; <var>expectedSize</var>.
     * @return a list of strings.
     */
    public List<String> asStringList() {
        ArrayList<String> res = new ArrayList(expectedSize)
        withTokens(new Function<String,Object>() {
            ArrayList<String> l = res
            Object apply(String tok) {
                l.add(tok)
            }
        })
        return res
    }

}
