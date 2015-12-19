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
import com.google.common.base.Function;
import groovy.transform.CompileStatic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

/**
 * Tokenizes a field. Written Java because Groovy refused to compile my version
 * with primitive arrays.
 */
@CompileStatic
public class GzipFieldTokenizer {

    private Blob blob;
    private int expectedSize;

    public GzipFieldTokenizer(Blob blob, int expectedSize) {
        this.blob = blob;
        this.expectedSize = expectedSize;
    }

    private <T> T withReader(Function<Reader, T> action)
            throws IOException, SQLException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(blob.getBinaryStream()), Charsets.US_ASCII));

        try {
            return action.apply(reader);
        } finally {
            reader.close();
        }

    }

    private <T> T withScanner(final Function<Scanner, T> action)
            throws IOException, SQLException {
        return withReader(new Function<Reader, T>() {
            public T apply(Reader reader) {
                Scanner scanner = new Scanner(reader);
                return action.apply(scanner);
            }

        });
    }

    public double[] asDoubleArray() throws NumberFormatException, IOException,
            InputMismatchException, SQLException {
        return withScanner(new Function<Scanner, double[]>() {
            public double[] apply(Scanner scan) {
                double[] res = new double[expectedSize];
                int i = 0;
                while (scan.hasNext()) {
                    if (i > expectedSize - 1) {
                        throw new InputMismatchException(
                                "Got more tokens than the " +
                                        expectedSize + " expected");
                    }

                    // do not use parseDouble, otherwise the scanner will just
                    // refuse to consume input that doesn't look like a float
                    String nextToken = scan.next();
                    res[i++] = Double.parseDouble(nextToken);
                }

                if (i < expectedSize) {
                    throw new InputMismatchException("Expected " +
                            expectedSize +
                            " tokens, but got only " + (i - 1));
                }

                return res;
            }
        });
    }

    public char[] asCharArray() throws IOException, InputMismatchException, SQLException {
        final char[] res = new char[expectedSize];
        final char[] pair = new char[2];
        return withReader(new Function<Reader, char[]>() {
            public char[] apply(Reader r) {
                int n;
                int i = 0;
                try {
                    while ((n = r.read(pair)) > 0) {
                        if (i > expectedSize - 1) {
                            throw new InputMismatchException(
                                    "Got more tokens than the " +
                                            expectedSize + " expected");
                        }

                        res[i++] = pair[0];
                        if (n == 2 && pair[1] != ' ') {
                            throw new InputMismatchException(
                                    "Found non-space character at position " +
                                            i + " with value \'" + pair[1] + "\'");
                        }

                    }
                } catch (IOException e) {
                    throw new UndeclaredThrowableException(e);
                }


                if (i < expectedSize) {
                    throw new InputMismatchException("Expected " +
                            expectedSize + " tokens, but got only " +
                            String.valueOf(i - 1));
                }


                return res;
            }

        });
    }

    /**
     * @throws InputMismatchException iff the number of values read &ne; <var>expectedSize</var>.
     * @return a list of strings.
     */
    public List<String> asStringList() throws IOException, InputMismatchException, SQLException {
        List<String> result = withReader(new Function<Reader, List<String>>() {
            public List<String> apply(Reader r) {
                ArrayList<String> res = new ArrayList<String>(expectedSize);
                StringBuilder builder = new StringBuilder();
                int n;
                char c;
                boolean nonempty = false;
                try {
                    while ((n = r.read()) > 0) {
                        nonempty = true;
                        c = (char) n;
                        if (c == ' ') {
                            res.add(builder.toString());
                            builder.setLength(0);
                        } else {
                            builder.append(c);
                        }
                    }
                } catch (IOException e) {
                    throw new UndeclaredThrowableException(e);
                }
                if (nonempty) {
                    res.add(builder.toString());
                }
                return res;
            }
        });
        if (result.size() != expectedSize) {
            throw new InputMismatchException("Expected " + expectedSize + " tokens, but got " + result.size());
        }
        return result;
    }

}
