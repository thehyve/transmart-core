package org.transmartproject.db.dataquery.highdim.acgh

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint

class AcghSegmentConstraint implements CriteriaDataConstraint {

    String chromosome
    Long   start,
           end

    //after construction, chromosome || (start && end)

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        criteria.with {
            and {
                if (chromosome) {
                    eq 'region.chromosome', chromosome
                }

                if (start  != null && end != null) {
                    or {
                        between 'region.start', start, end
                        between 'region.end',   start, end
                        and {
                            le 'region.start', start
                            ge 'region.end',   end
                        }
                    }
                }
            }
        }
    }
}
