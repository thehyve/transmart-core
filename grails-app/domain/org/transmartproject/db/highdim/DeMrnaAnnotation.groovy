package org.transmartproject.db.highdim

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class DeMrnaAnnotation implements Serializable {

	String gplId
	String probeId
	String geneSymbol
	BigDecimal probesetId
	Long geneId
	String organism

	int hashCode() {
		def builder = new HashCodeBuilder()
		builder.append gplId
		builder.append probeId
		builder.append geneSymbol
		builder.append probesetId
		builder.append geneId
		builder.append organism
		builder.toHashCode()
	}

	boolean equals(other) {
		if (other == null) return false
		def builder = new EqualsBuilder()
		builder.append gplId, other.gplId
		builder.append probeId, other.probeId
		builder.append geneSymbol, other.geneSymbol
		builder.append probesetId, other.probesetId
		builder.append geneId, other.geneId
		builder.append organism, other.organism
		builder.isEquals()
	}

	static mapping = {
		id composite: ["gplId", "probeId", "geneSymbol", "probesetId", "geneId", "organism"]
		version false
	}

	static constraints = {
		gplId nullable: true, maxSize: 100
		probeId nullable: true, maxSize: 100
		geneSymbol nullable: true, maxSize: 100
		probesetId nullable: true
		geneId nullable: true
		organism nullable: true, maxSize: 200
	}
}
