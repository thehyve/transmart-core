package org.transmartproject.db.biomarker

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.biomarker.BioMarkerConstraint

interface BioMarkerCriteriaConstraint extends BioMarkerConstraint {

    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria)

}
