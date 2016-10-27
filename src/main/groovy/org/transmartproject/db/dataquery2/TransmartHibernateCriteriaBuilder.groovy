package org.transmartproject.db.dataquery2

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.InheritConstructors
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections

@InheritConstructors
class TransmartHibernateCriteriaBuilder extends HibernateCriteriaBuilder {

    ProjectionList projections(Closure c) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("call to projections( ) not supported here"));
        }

        //this.projectionList = Projections.projectionList();
        invokeClosureNode(callable);

        if (projectionList != null && projectionList.getLength() > 0) {
            criteria.setProjection(projectionList);
        }

        return projectionList;

    }

}
