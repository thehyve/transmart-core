/*
 * Copyright © 2013-2014 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.chromoregion

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint

class ChromosomeSegmentConstraint implements CriteriaDataConstraint {

    //initialized with ChromosomeSegmentConstraintFactory
    String regionPrefix
    String regionChromosomeColumn
    String regionStartColumn
    String regionEndColumn
    boolean forceBigDecimal

    String chromosome
    Long   start,
           end

    //after construction, chromosome || (start && end)

    private n(v) {
        forceBigDecimal ? BigDecimal.valueOf(v) : v
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        // NOTE: depends on the alias of DeChromosomalRegion in the Criteria
        //       being called 'region'
        criteria.with {
            and {
                if (chromosome) {
                    eq regionPrefix + regionChromosomeColumn, chromosome
                }

                if (start != null && end != null) {
                    if (regionStartColumn == regionEndColumn) {
                        // optimization
                        between regionPrefix + regionStartColumn, n(start), n(end)
                    }

                    or {
                        between regionPrefix + regionStartColumn, n(start), n(end)
                        between regionPrefix + regionEndColumn,   n(start), n(end)
                        and {
                            le regionPrefix + regionStartColumn, n(start)
                            ge regionPrefix + regionEndColumn,   n(end)
                        }
                    }
                }
            }
        }
    }
}
