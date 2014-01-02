var HighDimensionalData = function () {

    /**
     * Get supported high dimensional data types
     * @returns {*}
     */
    var getSupportedTypes = function () {

        // check if tranSMART's global variable exists then use it
        if (typeof(HIGH_DIMENSIONAL_DATA) != "undefined") {
            return HIGH_DIMENSIONAL_DATA
        }

        return {
            "mrna": {"platform": "MRNA_AFFYMETRIX", "type": "Gene Expression"},
            "mirna_qpcr": {"platform": "MIRNA_QPCR", "type": "MIRNA_QPCR"},
            "mirna_seq": {"platform": "MIRNA_SEQ", "type": "MIRNA_SEQ"},
            "rbm": {"platform": "RBM", "type": "RBM"},
            "proteomics": {"platform": "PROTEIN", "type": "PROTEOMICS"},
            "snp": {"platform": "SNP", "type": "SNP"},
            "rnaseq": {"platform": "RNA_AFFYMETRIX", "type": "RNASEQ"}
        };
    }

    // define supported types
    this.supportedTypes = getSupportedTypes();

    // high dimensional data view element
    this.view = null;

    // high dimensional data
    this.data = null;

}

HighDimensionalData.prototype.populate_data = function () {

    // set global marker type
    GLOBAL.HighDimDataType = this.data.platforms[0].markerType;

    if (document.getElementById("highDimContainer")) {

        document.getElementById("gpl1").value = this.data.platforms[0].id;
        document.getElementById("sample1").value = this.data.sampleTypes[0].label;
        document.getElementById("tissue1").value = this.data.tissueTypes[0].label;

        this.create_pathway_search_box('searchPathway', 'divpathway');
    }
}

HighDimensionalData.prototype.create_pathway_search_box = function (searchInputEltName, divName) {

    var ajaxurl, ds, resultTpl;

    // remove all elements
    var el = document.getElementById(searchInputEltName);
    if (el) {
        el.value = '';   // empty the search input value
        // then remove all child elements
        while (el.firstChild) {
            el.removeChild(el.firstChild);
        }
    }

    ajaxurl = pageInfo.basePath + '/search/loadSearchPathways';
    ds = new Ext.data.Store({
        proxy: new Ext.data.ScriptTagProxy ({
            url: ajaxurl
        }),
        reader: new Ext.data.JsonReader(
            {root: "rows", id: "id"},
            [
                {name: "id"},
                {name: "source"},
                {name: "keyword"},
                {name: "synonyms"},
                {name: "category"},
                {name: "display"}
            ]
        )
    });

    // Custom rendering Template
    resultTpl = new Ext.XTemplate(
        '<tpl for=".">',
        '<div class="search-item">',
        '<p>',
        '<span class="category-{display:lowercase}">{display}&gt;{source}</span>&nbsp;',
        '<b>{keyword}</b>&nbsp; {synonyms}',
        '</p>',
        '</div>',
        '</tpl>'
    );

    var search = new Ext.form.ComboBox({
        store: ds,
        displayField: 'title',
        width: 455,
        typeAhead: false,
        loadingText: 'Searching...',
        listHeight: 500,
        valueField: 'naturalid',
        hideTrigger: true,
        allowBlank: false,
        name: 'searchText',
        mode: 'remote',
        tpl: resultTpl,
        minChars: 1,
        applyTo: searchInputEltName,
        itemSelector: 'div.search-item',

        onSelect: function (record) { // override default onSelect to do redirect
            var sp = Ext.get(searchInputEltName);
            sp.dom.value = record.data.keyword;
            GLOBAL.CurrentPathway = record.data.id;
            GLOBAL.CurrentPathwayName = record.data.keyword;
            search.collapse();
        }
    });

    if (GLOBAL.HeatmapType == 'Select' || GLOBAL.HeatmapType == 'PCA') {
        //Clear the pathway variable so we don't submit a value.
        GLOBAL.CurrentPathway = '';
        //Remove the pathway box.
        document.getElementById(divName).style.display = "none";
    }
}

HighDimensionalData.prototype.generate_view = function () {

    var _view = this.view;

    /**
     *  Inner function to display node details summary
     * @private
     */
    var _display_high_dim_selection_summary = function () {

        // init summary string
        var summaryString = '<br> <b>GPL Platform:</b> ' + Ext.get('gpl1').dom.value +
                            '<br> <b>Sample:</b> ' + Ext.get('sample1').dom.value +
                            '<br> <b>Tissue:</b> ' + Ext.get('tissue1').dom.value +
                            '<br>';

        // get search gene/pathway
        var selectedSearchPathway = Ext.get('searchPathway').dom.value;

        // create final string
        var innerHtml = summaryString +
            '<br> <b>Pathway:</b> ' + selectedSearchPathway +
            '<br> <b>Marker Type:</b> ' + GLOBAL.HighDimDataType;

        // display it
        var domObj = document.getElementById("display" + GLOBAL.CurrentAnalysisDivId);
        domObj.innerHTML = innerHtml;
    };

    /**
     * Inner function to create High Dimensional Popup element
     * @returns {Ext.Window}
     * @private
     */
    var _create_view = function () {
        return new Ext.Window({

            id: 'compareStepPathwaySelectionWindow',
            title: 'Compare Subsets-Pathway Selection',
            layout: 'fit',
            width: 475,
            autoHeight: true,
            closable: false,
            plain: true,
            modal: true,
            border: false,
            buttons: [
                {
                    id: 'dataAssociationApplyButton',
                    text: 'Apply Selections',
                    handler: function () {
                        _display_high_dim_selection_summary();
                        _view.hide();
                    }
                },
                {
                    text: 'Cancel',
                    handler: function () {
                        _view.hide();
                    }
                }
            ],
            resizable: false,
            autoLoad: {
                url: pageInfo.basePath + '/static/panels/highDimensionalWindow.html',
                scripts: true,
                nocache: true,
                discardUrl: true,
                method: 'GET'
            },
            tools: [
                {
                    id: 'help',
                    qtip: 'Click for context sensitive help',
                    handler: function (event, toolEl, panel) {
                        D2H_ShowHelp('1126', helpURL, "wndExternal", CTXT_DISPLAY_FULLHELP);
                    }
                }
            ]
        });
    }

    // ------------------------------------------- //
    // create view only when it's not created yet. //
    // ------------------------------------------- //

    if (!_view) {
        _view = _create_view();
    }

    return _view;
}

HighDimensionalData.prototype.get_inputs = function (divId) {
    return [{
        "label" : "High Dimensional Data",
        "el" : Ext.get(divId),
        "validations" : [
            {type:"REQUIRED"},
            {type:"HIGH_DIMENSIONAL"}
        ]
    }]
}

HighDimensionalData.prototype.gather_high_dimensional_data = function (divId) {

    var _this = this;

    /**
     * Reset global variables
     * @private
     */
    var _reset_global_var = function () {

        // reset the pathway information.
        GLOBAL.CurrentPathway = '';
        GLOBAL.CurrentPathwayName = '';

        // set global div id
        GLOBAL.CurrentAnalysisDivId = divId;

    }

    _reset_global_var();

    // check if global subset id is already defined or not
    // if not the re-run query
    if (!variableDivEmpty(divId)
        && ((GLOBAL.CurrentSubsetIDs[1] == null) || (multipleSubsets() && GLOBAL.CurrentSubsetIDs[2] == null))) {
        runAllQueriesForSubsetId(function () {
            _this.gather_high_dimensional_data(divId);
        }, divId);
        return;
    }

    // reset data
    _this.data = null;

    // instantiate input elements object with their corresponding validations
    var inputArray = this.get_inputs(divId);
    // define the validator for this form
    var formValidator = new FormValidator(inputArray);

    if (formValidator.validateInputForm()) {

        // get nodes from the dropzone
        var _nodes = Ext.get(divId).dom.childNodes;
        var _node = _nodes[0].concept.key;   // take the first node with assumption remaining nodes will have same
                                             // marker type

        Ext.Ajax.request({
            url: pageInfo.basePath + "/HighDimension/nodeDetails",
            method: 'POST',
            timeout: '1800000',
            params: Ext.urlEncode({
                conceptKey: _node
            }),
            success: function (result) {
                var _node_detail = JSON.parse(result.responseText);
                for (var key in _node_detail) {
                    if (_node_detail.hasOwnProperty(key)) {

                        // set data
                        _this.data = _node_detail[key];
                        _this.display_high_dimensional_popup();

                    } else {
                        Ext.Msg.alert("Error", "Unknown returned object.");
                    }
                }
            },
            failure: function () {
                Ext.Msg.alert("Error", "Cannot retrieve high dimensional node details");
            }
        });

    } else { // something is not correct in the validation
        // display the error message
        formValidator.display_errors();
    }
}

HighDimensionalData.prototype.display_high_dimensional_popup = function () {

    // generate view and populate it with the data
    this.view = this.generate_view();

    // then show it
    if (typeof viewport !== undefined) {
        this.view.show(viewport, this.populate_data());
    } else {
        console.error("No viewport to display the window.");
    }

}

var highDimensionalData = new HighDimensionalData();
