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

import com.google.common.base.Function
import com.google.common.collect.Iterators
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TypeCheckingMode
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.Projection

import java.sql.Blob
import java.sql.Clob

/**
 * The data row type returned by the snp_lz data type.
 * Only supports one row.
 */
@EqualsAndHashCode(includes=['probeData', 'orderedAssayPatientIndex', 'projection'])
@CompileStatic
class SnpLzRow<T> implements BioMarkerDataRow<T> {

    private Map<String, Object> probeData

    private int numberOfPatientsInTrial

    /**
     * Association between a certain assay and the position of the data for the
     * assay in the {gps,gts,dose}_by_probe_blob.
     * The order of the assays is the same as the one implied by
     * resultIndexPatientIndex, that is,
     * resultIndexPatientIndex[i] == orderedAssayPatientIndex's (i + 1)-th element
     */
    private LinkedHashMap<AssayColumn, Integer> orderedAssayPatientIndex

    /**
     * Map kept to optimize the calls to snpLzRow[i]. The same as
     * def i = 0; assayPatient.*value as int[]
     */
    private int[] resultIndexPatientIndex

    private Projection<? extends T> projection

    @Lazy
    double[] gpsByProbe = {
        if (probeData.gpsByProbeBlob == null) {
            return
        }
        new GzipFieldTokenizer(
                (Blob) probeData.gpsByProbeBlob,
                numberOfPatientsInTrial * 3).asDoubleArray()
    }()

    @Lazy
    List<String> gtsByProbe = {
        if (probeData.gtsByProbeBlob == null) {
            return
        }
        new GzipFieldTokenizer(
                (Blob) probeData.gtsByProbeBlob,
                numberOfPatientsInTrial * 2).asTokenList()
    }()

    @Lazy
    double[] doseByProbe = {
        if (probeData.doseByProbeBlob == null) {
            return
        }
        new GzipFieldTokenizer(
                (Blob) probeData.doseByProbeBlob,
                numberOfPatientsInTrial * 1).asDoubleArray()
    }()

    SnpLzRow(Map<String, Object> probeData,
             int numberOfPatientsInTrial,
             LinkedHashMap<AssayColumn, Integer> orderedAssayPatientIndex,
             int[] resultIndexPatientIndex,
             Projection<? extends T> projection) {
        this.probeData = probeData
        this.numberOfPatientsInTrial = numberOfPatientsInTrial
        this.orderedAssayPatientIndex = orderedAssayPatientIndex
        this.resultIndexPatientIndex = resultIndexPatientIndex
        this.projection = projection
    }

    @Override
    String getBioMarker() {
        /* has data such as EPHB2:2048|MIR4253:100422914 */
        def geneInfo = (String) probeData.geneInfo
        if (geneInfo) {
            geneInfo
                    .split(/\|/)*.split(/:/)
                    .collect { String[] strings ->
                        strings.first()
                    }
                    .join('/')
        }
    }

    @Override
    String getLabel() {
        snpName
    }

    private SnpLzAllDataCell getAtPatientIndex(int i) {
        if (i >= numberOfPatientsInTrial || i < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "Invalid patient index $i, there are " +
                            "$numberOfPatientsInTrial in the trial")
        }

        new SnpLzAllDataCell(
                gpsByProbe, gtsByProbe, doseByProbe, i)
    }

    @Override
    T getAt(int i) {
        projection.doWithResult(
            getAtPatientIndex(resultIndexPatientIndex[i]))
    }

    @Override
    T getAt(AssayColumn assayColumn) {
        Integer i = orderedAssayPatientIndex[assayColumn]
        projection.doWithResult(getAtPatientIndex(i))
    }

    @Override
    Iterator<T> iterator() {
        Iterators.transform(
                orderedAssayPatientIndex.values().iterator(),
                { int i -> projection.doWithResult(getAtPatientIndex(i)) } as Function<Integer, T>)
    }

    String getChromosome() {
        (String) probeData.chromosome
    }
    Long getPosition() {
        (Long) probeData.position
    }

    String getSnpName() {
        (String) probeData.snpName
    }

    String getA1() {
        probeData.a1 ?: ((Clob) probeData.a1Clob).characterStream.text
    }

    String getA2() {
        probeData.a2 ?: ((Clob) probeData.a2Clob).characterStream.text
    }

    BigDecimal getImputeQuality() {
        (BigDecimal) probeData.imputeQuality
    }

    BigDecimal getGTProbabilityThreshold() {
        (BigDecimal) probeData.gtProbabilityThreshold
    }

    BigDecimal getMinorAlleleFrequency() {
        (BigDecimal) probeData.maf
    }

    String getMinorAllele() {
        probeData.minorAllele.toString()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Long getA1a1Count() {
        probeData.CA1A1.longValue()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Long getA1a2Count() {
        (probeData.CA1A2).longValue()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Long getA2a2Count() {
        probeData.CA2A2.longValue()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Long getNoCallCount() {
        probeData.CNocall.longValue()
    }

}
