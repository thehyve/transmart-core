
var RmodulesView = function () {}

RmodulesView.prototype.clear_high_dimensional_input = function (div) {
    //Clear the drag and drop div.
    var qc = Ext.get(div);

    for (var i = qc.dom.childNodes.length - 1; i >= 0; i--) {
        var child = qc.dom.childNodes[i];
        qc.dom.removeChild(child);
    }
    clearHighDimDataSelections(div);
    clearSummaryDisplay(div);
}

RmodulesView.prototype.register_drag_drop = function () {
    //Set up drag and drop for Dependent and Independent variables on the data association tab.
    //Get the Independent DIV
    var independentDiv = Ext.get("divIndependentVariable");

    dtgI = new Ext.dd.DropTarget(independentDiv, {ddGroup : 'makeQuery'});
    dtgI.notifyDrop =  dropOntoCategorySelection;
}