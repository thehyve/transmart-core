
var RmodulesView = function () {
    this.variablesTypes = ["divDependentVariable", "divIndependentVariable"];
}

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

    /**
     * Register div as drop zone
     * @param divId
     * @private
     */
    var _register_drop_zone = function (divId) {
        var _el, _dtgI;

        if (_el = Ext.get(divId)) {
            _dtgI = new Ext.dd.DropTarget(_el, {ddGroup : 'makeQuery'});
            _dtgI.notifyDrop =  dropOntoCategorySelection;
        } else {
        }
    }

    for (var i=0; i<this.variablesTypes.length; i++) { // register all as drop zone ..
        _register_drop_zone(this.variablesTypes[i]);
    }
}

RmodulesView.prototype.get_parameters_for_mrna = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['gene_signatures'] = null;
    constraints['dataConstraints']['genes'] = null;
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['pathways'] = null;
    constraints['dataConstraints']['proteins'] = null;
    constraints['dataConstraints']['homologenes'] = null;

    return constraints;
}

RmodulesView.prototype.read_concept_variables = function () {

    var _el;
    var _ontology_terms = [];

    /**
     * get ontology terms
     * @param el
     * @returns {Array}
     * @private
     */
    var _add_ontology_terms = function (el, type) {
        var _type;

        for (var i=0; i < el.dom.childNodes.length; i++) {

            var _term = getQuerySummaryItem(el.dom.childNodes[i]).trim();

            // map the term type
            switch (type) {
                case 'divDependentVariable':
                    _type = 'dependent';
                    break;
                case 'divIndependentVariable':
                    _type = 'independent';
                    break;
                default :
                    _type = 'default';
                    break;
            }

            _ontology_terms.push({
                'term' : _term,
                'options' : {'type' : _type}
            });
        }
    }

    // register all as drop zone ..
    for (var i=0; i<this.variablesTypes.length; i++) {
        if (_el = Ext.get(this.variablesTypes[i])) {
            _add_ontology_terms(_el, this.variablesTypes[i]);
        }
    }

    return _ontology_terms;
}



RmodulesView.prototype.get_parameters_for_mirna = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['mirna'] = null;

    return constraints;
}

RmodulesView.prototype.get_parameters_for_rbm = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['genes'] = null;
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['proteins'] = null;

    return constraints;
}

RmodulesView.prototype.get_analysis_constraints = function (jobType) {

    var _data_type = GLOBAL.HighDimDataType;
    var _returnVal;

    var _this = this;

    // construct constraints object
    var _get_constraints_obj = function () {

        var _ontology_terms = _this.read_concept_variables();

        return  {
            "job_type" : jobType,
            "data_type": _data_type,
            "assayConstraints": {
                "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                "assay_id_list": null,
                "ontology_term": _ontology_terms,
                "trial_name": null
            },
            "dataConstraints": {
                "disjunction": null
            },
            "projections": ["default_real_projection"]
        }
    }
    _returnVal =  _get_constraints_obj();

    // do not create search_keyword_ids param if  pathway / gene is not selected
    // when gene / pathway is not selected, analysis will take up all pathways/genes.
    if (GLOBAL.CurrentPathway) {
        _returnVal['dataConstraints']['search_keyword_ids'] = [GLOBAL.CurrentPathway];
    }

    var cases =  {
        'Gene Expression':this.get_parameters_for_mrna,
        'MIRNA_QPCR':this.get_parameters_for_mirna,
        'MIRNA_SEQ':this.get_parameters_for_mirna,
        'RBM':this.get_parameters_for_rbm,
        'PROTEOMICS':this.get_parameters_for_rbm,
        'RNASEQ':this.get_parameters_for_mrna
    }

    if (cases[_data_type]) {
       _returnVal =  cases[_data_type](_returnVal);
    }

    return _returnVal;

}

RmodulesView.prototype.drop_onto_bin = function (source, e, data) {
    console.log("Dropping onto bin ..", data)
    this.el.appendChild(data.ddel);
    return true;
}
