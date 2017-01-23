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

class Log {
    static void err(message) {
        System.err.println "\u001b[1;31mERROR\u001b[m: \u001b[1m$message\u001b[m"
    }
    static void warn(message) {
        System.err.println "\u001b[1;33mWARN\u001b[m: \u001b[1m$message\u001b[m"
    }
    static void out(message) {
        System.out.println message
    }
    static void print(message) {
        System.out.print message
    }
}
