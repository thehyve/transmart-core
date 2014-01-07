package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn

class AssayColumnImpl implements AssayColumn {

    @Delegate
    private Assay assay

    final String label

    public AssayColumnImpl(Assay assay) {
        this.assay = assay
        this.label = assay.sampleCode
    }



    @Override
    public java.lang.String toString() {
        return "AssayColumnImpl{" +
                "assay=" + assay +
                ", label='" + label + '\'' +
                '}';
    }
}
