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

package org.transmartproject.db.dataquery.highdim.snp_lz;

import com.google.common.base.Objects;

/**
 * Container holding the values of {GPS,GTS,DOSE}_BY_PROBE_BLOB of
 * a specific sample.
 */
final class SnpLzCell {

    public final double probabilityA1A1;
    public final double probabilityA1A2;
    public final double probabilityA2A2;

    public final char likelyAllele1;
    public final char likelyAllele2;

    public final double minorAlleleDose;

    public SnpLzCell(double probabilityA1A1,
                     double probabilityA1A2,
                     double probabilityA2A2,
                     char likelyAllele1,
                     char likelyAllele2,
                     double minorAlleleDose) {
        this.probabilityA1A1 = probabilityA1A1;
        this.probabilityA1A2 = probabilityA1A2;
        this.probabilityA2A2 = probabilityA2A2;
        this.likelyAllele1 = likelyAllele1;
        this.likelyAllele2 = likelyAllele2;
        this.minorAlleleDose = minorAlleleDose;
    }

    public double getProbabilityA1A1() {
        return probabilityA1A1;
    }

    public double getProbabilityA1A2() {
        return probabilityA1A2;
    }

    public double getProbabilityA2A2() {
        return probabilityA2A2;
    }

    public char getLikelyAllele1() {
        return likelyAllele1;
    }

    public char getLikelyAllele2() {
        return likelyAllele2;
    }

    public double getMinorAlleleDose() {
        return minorAlleleDose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SnpLzCell snpLzCell = (SnpLzCell) o;
        return Objects.equal(probabilityA1A1, snpLzCell.probabilityA1A1) &&
                Objects.equal(probabilityA1A2, snpLzCell.probabilityA1A2) &&
                Objects.equal(probabilityA2A2, snpLzCell.probabilityA2A2) &&
                Objects.equal(likelyAllele1, snpLzCell.likelyAllele1) &&
                Objects.equal(likelyAllele2, snpLzCell.likelyAllele2) &&
                Objects.equal(minorAlleleDose, snpLzCell.minorAlleleDose);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                probabilityA1A1,
                probabilityA1A2,
                probabilityA2A2,
                likelyAllele1,
                likelyAllele2,
                minorAlleleDose);
    }
}
