/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
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
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package inc.oracle

/**
 * Break .sql files into statements that can be used in JDBC.
 * It's a very basic parser; not guaranteed to work with all SQL files
 */
class SqlSplitter {

    BufferedReader reader

    SqlSplitter(Reader reader) {
        this.reader = new BufferedReader(reader)
    }

    SqlSplitter(BufferedReader reader) {
        this.reader = reader
    }

    void each(Closure closure) {
        String statement
        while ((statement = nextStatement) != null) {
            closure.call statement
        }
    }

    String getNextStatement() {
        def collected = new LinkedList<String>()
        State state = State.BEFORE_STATEMENT
        String line

        loop:
        while ((line = reader.readLine()) != null) {
            def trimmedLine = line.trim()
            switch (state) {
                case State.BEFORE_STATEMENT:
                    if (trimmedLine.startsWith('--')) {
                        continue /* other types of comments not supported */
                    }
                    if (!trimmedLine) {
                        continue
                    }
                    state = State.INSIDE_REGULAR_STATEMENT
                    /* break intentionally omitted */
                case State.INSIDE_REGULAR_STATEMENT:
                    if (trimmedLine.toLowerCase() == 'as'
                            || trimmedLine.toLowerCase() == 'begin'
                            || trimmedLine.toLowerCase() == 'declare'
                            || trimmedLine.startsWith('CREATE OR REPLACE TRIGGER')
                            || trimmedLine.startsWith('CREATE OR REPLACE TYPE')
                            || trimmedLine.startsWith('CREATE OR REPLACE PROCEDURE')
                            || trimmedLine.startsWith('CREATE OR REPLACE FUNCTION')) {
                        state = State.INSIDE_FUNCTION
                    } else if (trimmedLine.endsWith(';')) {
                        collected << trimmedLine.substring(0, trimmedLine.length() - 1) /* remove semicolon */
                        break loop
                    }
                    collected << line
                    break
                case State.INSIDE_FUNCTION:
                    if (line == '/') {
                        break loop
                    }
                    collected << line
                    break
            }
        }

        collected.join('\n') ?: null
    }

    enum State {
        BEFORE_STATEMENT,
        INSIDE_REGULAR_STATEMENT,
        INSIDE_FUNCTION /* or procedure */
    }

}
