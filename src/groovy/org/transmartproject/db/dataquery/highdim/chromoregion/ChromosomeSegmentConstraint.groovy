package org.transmartproject.db.dataquery.highdim.chromoregion

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint

class ChromosomeSegmentConstraint implements CriteriaDataConstraint {

    //initialized with ChromosomeSegmentConstraintFactory
    String regionPrefix
    String regionChromosomeColumn
    String regionStartColumn
    String regionEndColumn

    String chromosome
    Long   start,
           end

    //after construction, chromosome || (start && end)

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        // NOTE: depends on the alias of DeChromosomalRegion in the Criteria
        //       being called 'region'
        criteria.with {
            and {
                if (chromosome) {
                    eq regionPrefix+regionChromosomeColumn, chromosome
                }

                if (start  != null && end != null) {
                    or {
                        between regionPrefix+regionStartColumn, start, end
                        between regionPrefix+regionEndColumn,   start, end
                        and {
                            le regionPrefix+regionStartColumn, start
                            ge regionPrefix+regionEndColumn,   end
                        }
                    }
                }
            }
        }
    }
}
