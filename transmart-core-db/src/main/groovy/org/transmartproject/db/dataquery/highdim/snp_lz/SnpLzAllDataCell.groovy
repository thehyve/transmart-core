/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Method

/**
 * Calculates the values of {GPS,GTS,DOSE}_BY_PROBE_BLOB of
 * a specific sample using the probe data.
 *
 * Depending on the projection, not all will be filled in.
 *
 * This type will only be returned to the retrieveData() caller if the all data
 * projection was requested. Otherwise, it will be transformed into another
 * type.
 *
 * Needs to implement map in order to satisfy the {@link AllDataProjection}
 * contract.
 */
@CompileStatic
class SnpLzAllDataCell implements Map<String, Object> {

    static final String separator = Character.toString((char) 0x5F /* _ */)

    private double[] gpsByProbe
    private List<String> gtsByProbe
    private double[] doseByProbe

    private int i

    SnpLzAllDataCell(double[] gpsByProbe, List<String> gtsByProbe, double[] doseByProbe, int i) {
        this.gpsByProbe = gpsByProbe
        this.gtsByProbe = gtsByProbe
        this.doseByProbe = doseByProbe
        this.i = i
    }

    private String getLikelyAllele1() {
        gtsByProbe[i * 2]
    }

    private String getLikelyAllele2() {
        gtsByProbe[i * 2 + 1]
    }

    String getLikelyGenotype() {
        return likelyAllele1.concat(separator).concat(likelyAllele2)
    }

    double getProbabilityA1A1() {
        gpsByProbe[i * 3]
    }
    double getProbabilityA1A2() {
        gpsByProbe[i * 3 + 1]
    }

    double getProbabilityA2A2() {
        gpsByProbe[i * 3 + 2]
    }

    double getMinorAlleleDose() {
        doseByProbe[i]
    }

    /**********************
     * Map implementation *
     **********************/
    private static Map<String, Method> PROPERTIES = {
        Introspector.getBeanInfo(SnpLzAllDataCell).propertyDescriptors
                .collectEntries { PropertyDescriptor pd ->
            [(pd.name): pd.readMethod]
        }
    }()


    @Override
    int size() {
        PROPERTIES.size()
    }

    @Override
    boolean isEmpty() {
        false
    }

    @Override
    boolean containsKey(Object key) {
        PROPERTIES.containsKey(key)
    }

    @Override
    boolean containsValue(Object value) {
        throw new UnsupportedOperationException()
    }

    @Override
    Object get(Object key) {
        if (!containsKey(key)) {
            return null
        }

        PROPERTIES[(String) key].invoke(this)
    }

    @Override
    Object put(String key, Object value) {
        throw new UnsupportedOperationException()
    }

    @Override
    Object remove(Object key) {
        throw new UnsupportedOperationException()
    }

    @Override
    void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException()
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException()
    }

    @Override
    Set<String> keySet() {
        PROPERTIES.keySet()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<Object> values() {
        PROPERTIES.collect {
            this."$it"
        }
    }

    @Override
    Set<Map.Entry<String, Object>> entrySet() {
        /*
        Maps.transformEntries(PROPERTIES, { key, value ->
            this.get(key)
        } as Maps.EntryTransformer).entrySet()*/
    }
}
