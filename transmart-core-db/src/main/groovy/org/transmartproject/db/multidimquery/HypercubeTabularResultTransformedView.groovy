package org.transmartproject.db.multidimquery

import org.transmartproject.core.dataquery.TabularResult

class HypercubeTabularResultTransformedView implements TabularResult<HypercubeDataColumn, HypercubeDataRow> {

    final TabularResult<HypercubeDataColumn, HypercubeDataRow> originalView

    HypercubeTabularResultTransformedView(TabularResult<HypercubeDataColumn, HypercubeDataRow> hypercubeTabularResultView) {
        originalView = hypercubeTabularResultView
    }

    @Override
    List<HypercubeDataColumn> getIndicesList() {
        return null
    }

    @Override
    Iterator<HypercubeDataRow> getRows() {
        iterator()
    }

    @Override
    String getColumnsDimensionLabel() {
        return null
    }

    @Override
    String getRowsDimensionLabel() {
        return null
    }

    @Override
    void close() throws IOException {
        originalView.close()
    }

    @Override
    Iterator<HypercubeDataRow> iterator() {
        return null
    }
}
