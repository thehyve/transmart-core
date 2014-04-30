package org.transmartproject.db.dataquery.highdim.vcf

import grails.orm.HibernateCriteriaBuilder

import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Created by j.hudecek on 21-2-14.
 */
class CohortProjection implements CriteriaProjection<Map> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {

    }

    @Override
    Map doWithResult(Object object) {
        if (object == null) {
            return null /* missing data for an assay */
        }

        // For computing the cohort properties, we need at least 
        // the allele1 and allele2 properties
        Map output = [:]
        
        // We receive an object with HashMapEntries, but will have 
        // to convert it into a map
        object.each { 
            output[ it.key ] = it.value
        }
        
        output
    }
}
