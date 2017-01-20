package selectors

class ObservationsMessageJson {

    public final dimensionDeclarations
    public final cells
    public final dimensionElements

    ObservationsMessageJson(dimensionDeclarations, cells, dimensionElements) {
        assert dimensionDeclarations != null
        assert cells != null
        assert dimensionElements != null
        this.dimensionDeclarations = dimensionDeclarations
        this.cells = cells
        this.dimensionElements = dimensionElements
    }
}
