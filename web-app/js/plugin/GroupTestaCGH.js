/**
 * User: riza
 * Date: 23-04-13
 * Time: 11:28
 */

/**
 * GLOBAL PARAMETERS FOR GENERIC COMPONENTS
 * @type {number}
 */

var groupTestView;

var GroupTestInputWidget = Ext.extend(GenericAnalysisInputBar, {

    regionPanel: null,
    groupPanel: null,
    statTestPanel: null,
    alterationPanel: null,

    statRadios: [
        {boxLabel: 'Chi-square', name: 'stat-test-opt', XValue: 'Chi-square'},
        {boxLabel: 'Wilcoxon', name: 'stat-test-opt', XValue: 'Wilcoxon'},
        {boxLabel: 'Kruskal-Wallis', name: 'stat-test-opt', XValue: 'KW'}
    ],

    alterationRadios: [
        {boxLabel: 'GAIN', name: 'cb-col', XValue: '1'},
        {boxLabel: 'LOSS', name: 'cb-col', XValue: '-1'},
        {boxLabel: 'BOTH', name: 'cb-col', XValue: '0'}
    ],

    constructor: function (config) {
        GroupTestInputWidget.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function () {

        // define child panel configs
        var childPanelConfig = [
            {
                title: 'Region',
                id: 'gt-input-region',
                isDroppable: true,
                notifyFunc: dropOntoCategorySelection,
                toolTipTitle: 'Tip: Region',
                toolTipTxt: 'Drag and drop aCGH region here.'
            },
            {
                title: 'Group',
                id: 'gt-input-group',
                isDroppable: true,
                notifyFunc: dropOntoCategorySelection,
                toolTipTitle: 'Tip: Group',
                toolTipTxt: 'Drag and drop clinical variables to associate copy number data with. Please keep in mind that only ' +
                    'one variable can be compared, e.g. gender (female) with gender (male); not gender (female) with age ' +
                    '(>60)!'
            },
            {
                title: 'Statistical Test',
                id: 'gt-input-stat-test',
                toolTipTitle: 'Tip: Statistical Test',
                toolTipTxt: '<ul><li><i>Chi-square</i>: test for association between aberration pattern and group label; can also do ' +
                    'multiple comparisons</li> <li><i>Wilcoxon</i>: rank-sum test for two groups</li> <li><i>Kruskal-Wallis</i>: generalisation of ' +
                    'Wilcoxon for more than two groups</li></ul>'
            },
            {
                title: 'Alteration Type',
                id: 'gt-input-alteration',
                toolTipTitle: 'Tip: Alteration Type',
                toolTipTxt: 'Select type of chromosomal alteration to test the association.'
            }
        ];

        // create child panels
        this.regionPanel = this.createChildPanel(childPanelConfig[0]);
        this.groupPanel = this.createChildPanel(childPanelConfig[1]);
        this.statTestPanel = this.createChildPanel(childPanelConfig[2]);
        this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

        // create check boxes
        this.statTestPanel.add(this.createRadioBtnGroup(this.statRadios, 'stat-test-chk-group'));
        this.alterationPanel.add(this.createRadioBtnGroup(this.alterationRadios, 'alteration-types-chk-group'));

        // re-draw
        this.doLayout();
    }
});

/**
 * list of result grid columns
 * @type {Array}
 * @private
 */
var _grouptestgrid_columns = [
    {
        id: 'chromosome', // id assigned so we can apply custom css (e.g. .x-grid-col-topic b { color:#333 })
        header: "chromosome",
        dataIndex: 'chromosome',
        width: 400,
        sortable: true
    },
    {
        header: "cytoband",
        dataIndex: 'cytoband',
        width: 100,
        sortable: true
    },
    {
        header: "start",
        dataIndex: 'start',
        width: 100,
        sortable: true
    },
    {
        header: "end",
        dataIndex: 'end',
        width: 100,
        sortable: true
    },
    {
        header: "pvalue",
        dataIndex: 'pvalue',
        width: 100,
        align: 'right',
        sortable: true
    },
    {
        id: 'fdr',
        header: "fdr",
        dataIndex: 'fdr',
        align: 'right',
        width: 100,
        sortable: true
    }
];

/**
 * This object represents the intermediate result grid
 * @type {*|Object}
 */
var GroupTestResultGrid = Ext.extend(GenericAnalysisResultGrid, {

    jobName: '',

    constructor: function (config) {
        GroupTestResultGrid.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function () {

    },

    /**
     * Get selected rows from the grid
     * @returns {*}
     */
    getSelectedRows: function () {
        return this.getSelectionModel().getSelections();
    },

    /**
     * Display selected rows in tab panel
     * @param selectedRegions
     */
    displaySurvivalPlot: function (selectedRegions) {

        var outer_scope = this;

        // creating tab panel
        if (this.plotCurvePanel == null) {
            this.plotCurvePanel = new GenericTabPlotPanel({
                id: 'plotResultCurve',
                renderTo: 'plotResultWrapper'
            });
        }
    }
});

var GroupTestResultGrid = Ext.extend(GenericAnalysisResultGrid, {

    jobName: '',

    constructor: function (config) {
        GroupTestResultGrid.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function () {

    },

    /**
     * Get selected rows from the grid
     * @returns {*}
     */
    getSelectedRows: function () {
        return this.getSelectionModel().getSelections();
    },

    downloadIntermediateResult: function (jobName) {

        // clean up
        try {
            Ext.destroy(Ext.get('downloadIframe'));
        }
        catch (e) {
        }

        // get the file
        Ext.DomHelper.append(document.body, {
            tag: 'iframe',
            id: 'downloadIframe',
            frameBorder: 0,
            width: 0,
            height: 0,
            css: 'display:none;visibility:hidden;height:0px;',
            src: pageInfo.basePath + "/analysisFiles/" + jobName + "/zippedData.zip"
        });
    }
});

/**
 * This class represents the whole Group Test view
 * @type {*|Object}
 */
var GroupTestView = Ext.extend(GenericAnalysisView, {

    // input panel
    inputBar: null,

    // result panel
    resultPanel: null,

    // alteration
    alteration: '',

    // job type
    jobType: 'aCGHgroupTest',

    // job info
    jobInfo: null,

    // constructor
    constructor: function () {
        this.init();
    },

    init: function () {

        // first of all, let's reset all major components
        this.resetAll();

        // draw input panel
        this.inputBar = new GroupTestInputWidget({
            id: 'gtInputPanel',
            title: 'Input Parameters',
            iconCls: 'newbutton',
            renderTo: 'gtContainer',
            bbar: this.createInputToolBar()
        });
    },

    redraw: function () {
        this.inputBar.doLayout();
    },

    resetAll: function () {
        this.tabIndex = 0;
        Ext.destroy(this.inputBar);
        Ext.destroy(this.resultPanel);
    },

    createInputToolBar: function () {
        var _this = this;

        /**
         * Buttons for Input Panel
         * @type {Array}
         */
        var gtInputBarBtnList = ['->', // '->' making it right aligned
            'Permutations:',
            {
                xtype: 'textfield',
                name: 'permutation',
                id: 'permutation',
                width: 50,
                value: 10000
            },
            {
                xtype: 'button',
                text: 'Run Analysis',
                scale: 'medium',
                iconCls: 'runbutton',
                handler: function () {
                    groupTestView.submitGroupTestJob();
                }
            }];

        return new Ext.Toolbar({
            height: 30,
            items: gtInputBarBtnList
        });
    },

    areAllMandatoryFieldsFilled: function () {
        var isValid = true;
        var invalidInputs = [];

        var statisticalVal;
        var alterationVal;

        // check if region panel is empty
        if (this.inputBar.regionPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.regionPanel.title);
            isValid = false;
        }

        // check if group panel is empty
        if (this.inputBar.groupPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.groupPanel.title);
            isValid = false;
        }

        //check if stat test values has been selected
        var statChkGroup = this.inputBar.statTestPanel.getComponent('stat-test-chk-group');
        statisticalVal = statChkGroup.getXValues();
        if (statisticalVal.length < 1) {
            isValid = false;
            invalidInputs.push(this.inputBar.statTestPanel.title);
        }

        //check if alteration values has been selected
        var alterationChkGroup = this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
        alterationVal = alterationChkGroup.getSelectedValue();

        if (!alterationVal) {
            isValid = false;
            invalidInputs.push(this.inputBar.alterationPanel.title);
        }

        var permutationEl = Ext.get('permutation');
        if (permutationEl.getValue().trim() == '' || isNaN(permutationEl.getValue())) {
            isValid = false;
            invalidInputs.push('Permutations');
        }

        if (!isValid) {
            var strErrMsg = 'Following needs to be defined: ';
            invalidInputs.each(function (item) {
                strErrMsg += '[' + item + '] ';
            })

            // inform user on mandatory inputs need to be defined
            Ext.MessageBox.show({
                title: 'Missing mandatory inputs',
                msg: strErrMsg,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });

        }

        return isValid;
    },

    isGroupFieldValid: function () {
        if (this.inputBar.groupPanel.getNumberOfConceptCodes() < 2) {
            Ext.MessageBox.show({
                title: 'Incorrect number of groups',
                msg: '[Group] input field should contain more than one value. Please add more values.',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });
            return false;
        }
        return true;
    },

    validateInputs: function () {
        return this.areAllMandatoryFieldsFilled() && this.isGroupFieldValid()
    },

    createResultPlotPanel: function (jobName, view) {

        var _this = view;

        // initialize image path
        var imagePath = '';


        // get image path
        Ext.Ajax.request({
            url: pageInfo.basePath + "/aCGHgroupTest/imagePath",
            method: 'POST',
            success: function (result, request) {

                imagePath = result.responseText;

                _this.resultPanel = new GenericPlotPanel({
                    id: 'plotResultCurve',
                    renderTo: 'gtPlotWrapper',
                    width: '100%',
                    frame: true,
                    height: 600,
                    defaults: {autoScroll: true}
                });

                // Getting the template as blue print for survival curve plot.
                // Template is defined in GroupTestaCGH.gsp
                var groupTestPlotTpl = Ext.Template.from('template-group-test-plot');

                var translatedAlteration = groupTestView.translateAlteration(
                    groupTestView.jobInfo.jobInputsJson.aberrationType
                );

                var groupVariable = groupTestView.jobInfo.jobInputsJson.groupVariable;
                var groupVariableHtml = groupVariable ? groupVariable.replace('|', '<br />') : '';
                // create data instance
                var region = {
                    filename: imagePath,
                    jobName: groupTestView.jobInfo.name,
                    startDate: groupTestView.jobInfo.startDate,
                    runTime: groupTestView.jobInfo.runTime,
                    inputRegion: groupTestView.jobInfo.jobInputsJson.regionVariable,
                    inputGroupVariable: groupVariableHtml,
                    inputStatisticsType: groupTestView.jobInfo.jobInputsJson.statisticsType,
                    inputCohort1: groupTestView.jobInfo.jobInputsJson.result_instance_id1,
                    inputCohort2: groupTestView.jobInfo.jobInputsJson.result_instance_id2,
                    inputAlteration: translatedAlteration
                };

                // generate template
                groupTestPlotTpl.overwrite(Ext.get('gtPlotWrapper'), region);

                jQuery.get(pageInfo.basePath + '/dataExport/isCurrentUserAllowedToExport',
                    {
                        result_instance_id1: groupTestView.jobInfo.jobInputsJson.result_instance_id1,
                        result_instance_id2: groupTestView.jobInfo.jobInputsJson.result_instance_id2
                    },
                    function(data) {
                        if (data.result) {
                            new Ext.Button({
                                text: 'Download Result',
                                iconCls: 'downloadbutton',
                                renderTo: 'downloadBtn',
                                handler: function () {
                                    _this.downloadGroupTestResult(jobName);
                                }
                            });
                        }
                    });
            },
            params: {
                jobName: jobName,
                alteration: _this.alteration
            }
        });
    },

    /**
     * generates intermediate result in grid panel
     * @param data
     */
    renderResults: function (jobName, view) {

        var _this = this;

        Ext.Ajax.request({
            // retrieve information about the job (status, inputs, run-time, etc)
            url: pageInfo.basePath + "/asyncJob/getjobbyname",
            method: 'GET',
            success: function (result, request) {

                var resultJSON = JSON.parse(result.responseText);
                _this.jobInfo = resultJSON.jobs[0];

                // create store data
                var store = new Ext.data.JsonStore({
                    root: 'result',
                    totalProperty: 'totalCount',
                    idProperty: 'threadid',
                    remoteSort: true,   // can be enhanced with remote sort

                    baseParams: {jobName: jobName},

                    fields: [
                        'chromosome',
                        'cytoband',
                        {name: 'start', type: 'int'},
                        {name: 'end', type: 'int'},
                        {name: 'pvalue', type: 'float'},
                        {name: 'fdr', type: 'float'}
                    ],

                    // load using script tags for cross domain, if the data in on the same domain as
                    // this page, an HttpProxy would be better
                    proxy: new Ext.data.HttpProxy({
                        url: pageInfo.basePath + "/aCGHgroupTest/resultTable"
                    })

                });
                store.setDefaultSort('chromosome', 'asc');


                // create paging bar with related store
                var pagingbar = new Ext.PagingToolbar({
                    pageSize: GEN_RESULT_GRID_LIMIT,
                    store: store,
                    displayInfo: true,
                    displayMsg: 'Displaying topics {0} - {1} of {2}',
                    emptyMsg: "No topics to display"
                });

                // make sure no instance from previous job
                Ext.destroy(Ext.get('intermediateGridPanel'));

                // create new grid and render it
                view.intermediateResultGrid = new GroupTestResultGrid({
                    id: 'intermediateGridPanel',
                    title: 'Intermediate Result - Job Name: ' + jobName,
                    renderTo: 'intermediateResultWrapper',
                    trackMouseOver: false,
                    loadMask: true,
                    columns: _grouptestgrid_columns,
                    store: store,
                    bbar: pagingbar,
                    jobName: jobName
                });

                view.intermediateResultGrid.render();

                // finally load the data
                store.load({params: {start: 0, limit: GEN_RESULT_GRID_LIMIT}});

                _this.createResultPlotPanel(jobName, view)
            },
            failure: function (result, request) {
                console.log('failure ....')
            },
            params: {
                jobName: jobName
            }
        })

    },

    downloadGroupTestResult: function (jobName) {

        // clean up
        try {
            Ext.destroy(Ext.get('downloadIframe'));
        }
        catch (e) {
        }

        // get the file
        Ext.DomHelper.append(document.body, {
            tag: 'iframe',
            id: 'downloadIframe',
            frameBorder: 0,
            width: 0,
            height: 0,
            css: 'display:none;visibility:hidden;height:0px;',
            src: pageInfo.basePath + "/analysisFiles/" + jobName + "/zippedData.zip"
        });
    },

    onJobFinish: function (jobName, view) {
        this.renderResults(jobName, view);
    },

    submitGroupTestJob: function () {
        var _this = this;

        // Fill global subset ids if null
        if ((!isSubsetEmpty(1) && GLOBAL.CurrentSubsetIDs[1] == null) ||
            (!isSubsetEmpty(2) && GLOBAL.CurrentSubsetIDs[2] == null)) {
            runAllQueries(function() {_this.submitGroupTestJob();});
            return;
        }

        if (this.validateInputs()) {
            var regionEl =  this.inputBar.regionPanel.getInputEl();
            var errorMessage = "One or more of the high dimensional data nodes are not acgh data; select a copy number regions file for this analysis";
            this.validateDataTypes(regionEl, ["acgh"], errorMessage, function() {

                var regionVal = _this.inputBar.regionPanel.getConceptCode();
                var groupVals = _this.inputBar.groupPanel.getConceptCodes();
                var statTestComponent = _this.inputBar.statTestPanel.getComponent('stat-test-chk-group');
                var statTestVal = statTestComponent.getSelectedValue();
                var alternationComponent = _this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
                var alternationVal = alternationComponent.getSelectedValue();
                var permutationComponent = Ext.get('permutation');
                var permutation = permutationComponent.getValue();

                _this.alteration = _this.translateAlteration(alternationVal);

                // create a string of all the concepts we need for the i2b2 data.
                var variablesConceptCode = regionVal;
                variablesConceptCode += groupVals != '' ? "|" + groupVals : "";

                // compose params
                var formParams = {
                    regionVariable: regionVal,
                    groupVariable: groupVals,
                    statisticsType: statTestVal,
                    aberrationType: alternationVal,
                    numberOfPermutations: permutation,
                    variablesConceptPaths: variablesConceptCode,
                    analysisConstraints: JSON.stringify({
                        "job_type": _this.jobType,
                        "data_type": "acgh",
                        "assayConstraints": {
                            "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                            "assay_id_list": null,
                            "ontology_term": [
                                {
                                    'term': regionVal,
                                    'options': {'type': "default"}
                                }
                            ],
                            "trial_name": null
                        },
                        "dataConstraints": {
                            "disjunction": null
                        },
                        "projections": ["acgh_values"]
                    }),
                    jobType: _this.jobType
                };

                var job = _this.submitJob(formParams, _this.onJobFinish, _this);
            });
        }

    }

});

/**
 * Invoked when user selects Group Test aCGH from Analysis combo box
 */
function loadGroupTestaCGHView() {
    // everything starts here ..
    groupTestView = new GroupTestView();
}
