package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube

class EmptyHypercube implements Hypercube{

    final boolean DimensionsPreloadable = false
    final boolean DimensionsPreloaded =  false
    final boolean AutoloadDimensions = false

    void loadDimensions() {}
    ImmutableList<Object> dimensionElements(Dimension dim){ [] as ImmutableList}

    ImmutableList<Dimension> getDimensions() {[] as ImmutableList}

    Object dimensionElement(Dimension dim, Integer idx) { null }

    Object dimensionElementKey(Dimension dim, Integer idx) { null }

    void close(){}

    void setAutoloadDimensions(boolean value){}

    void preloadDimensions(){}

    Iterator iterator(){
        return new Iterator() {
            @Override
            boolean hasNext() {
                return false
            }

            @Override
            Object next() {
                return null
            }
        }
    }


}
