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

package lib.clinical

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes = ['categoryCode', 'dataLabel'])
@ToString(includes = ['categoryCode', 'dataLabel'])
class Column implements Comparable<Column> {
    public final static String SUBJECT_ID_LABEL = 'SUBJ_ID'
    public final static String AGE_LABEL = 'AGE'
    public final static String SEX_LABEL = 'SEX'
    public final static String RACE_LABEL = 'RACE'

    private final Map PRIORITY_LABEL_MAP = [
            1: SUBJECT_ID_LABEL,
            2: AGE_LABEL,
            3: SEX_LABEL,
            4: RACE_LABEL
    ]

    String categoryCode
    String dataLabel
    String dataLabelSource = ''
    String controlledVocabularyCode = ''

    @Override
    int compareTo(Column other) {
        other.priority <=> priority ?:
                other.categoryCode <=> categoryCode ?:
                other.dataLabel <=> dataLabel
    }

    private Integer getPriority() {
        PRIORITY_LABEL_MAP[dataLabel]
    }
}
