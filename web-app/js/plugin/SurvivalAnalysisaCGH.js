/**
 * Created with IntelliJ IDEA.
 * User: riza
 * Date: 26-04-13
 * Time: 09:18
 * To change this template use File | Settings | File Templates.
 */


/**
 * dummy data for intermediate result
 */
var dummyData = [
	['Chr1:7282393-152722828',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:31432123-23232322',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:1232323-123344552',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:2323123-232312222',"1p36.31",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:55123123-62342342',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:7234324-234324343',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:23423443-26534343',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],

	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:514212-1232323222',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:51231233-22212332',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:8234342-343434333',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:623423433-1232323',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:24232321-12323232',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:12323233-71231232',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:22232323-12333223',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],

	['Chr1:62342342-23423444',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:1232323-234232444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-12323w23',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-24232321',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-55123123',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN']
];


/**
 * View instance
 */
var survivalAnalysisACGHView;

/**
 * Buttons for Input Panel
 * @type {Array}
 */
var saInputPanelBtnList = ['->',{  // '->' making it right aligned
	xtype: 'button',
	text: 'Run Analysis',
	scale: 'medium',
	iconCls: 'runbutton',
	handler: function () {
		survivalAnalysisACGHView.submitSurvivalAnalysisaCGHJob();
	}
}];

/**
 * Buttons for Intermediate Result Panel
 * @type {Array}
 */
var saIntermediatePanelBtnList = ['->',
	{
		xtype: 'button',
		text: 'Download Result',
		scale: 'medium',
		iconCls: 'downloadbutton',
		handler: function (b, e) {

			// ****************************************************************** //
			// TODO: To provide handler for download Intermediate result data     //
			// ****************************************************************** //

			console.log("LOG: about to download intermediate result data");
		}
	},
	{
		xtype: 'button',
		text: 'Show Survival Plot',
		scale: 'medium',
		iconCls: 'chartcurvebutton',
		handler: function (b, e) {

			// get selected regions
			var selectedRows = survivalAnalysisACGHView.getSelectedRows();
			if (selectedRows.length > 0) {
				// invoke display survival plot
				survivalAnalysisACGHView.displaySurvivalPlot(selectedRows);
			} else {
				// warn user if there's no row is selected
				Ext.MessageBox.show({
					title: 'No row is selected',
					msg: 'To display the survival plot, you must select at least one region from the grid.',
					buttons: Ext.MessageBox.OK,
					icon: Ext.MessageBox.INFO
				});
			}

		}
	}];

/**
 * This object represents Input Bar in the Survival Analysis page
 * @type {*|Object}
 */
var SurvivalAnalysisInputBar = Ext.extend(GenericAnalysisInputBar, {

	regionPanel: null,
	survivalPanel: null,
	censoringPanel: null,
	alterationPanel: null,

	alterationCheckboxes: [
		{boxLabel: 'GAIN vs NO GAIN', name: 'cb-col-1', XValue:'gain-no-gain'},
		{boxLabel: 'LOSS vs NO LOSS', name: 'cb-col-2', XValue:'loss-no-loss'},
		{boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'cb-col-3', XValue:'loss-normal-gain'}
	],

	constructor: function(config) {
		SurvivalAnalysisInputBar.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function() {

		// define child panel configs
		var childPanelConfig = [{
			title: 'Region',
			id:  'sa-input-region',
			isDroppable: true,
			notifyFunc: dropNumericOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Region'

		},{
			title: 'Survival Time',
			id:  'sa-input-survival',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Survival Time'

		},{
			title: 'Censoring Variable',
			id: 'sa-input-censoring',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Censoring variable'
		},{
			title: 'Alteration Type',
			id:  'sa-input-alteration',
			toolTipTxt: 'Tool-tip for Alteration Type'
		}];

		// create child panels
		this.regionPanel = this.createChildPanel(childPanelConfig[0]);
		this.survivalPanel = this.createChildPanel(childPanelConfig[1]);
		this.censoringPanel = this.createChildPanel(childPanelConfig[2]);
		this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

		// create check boxes
		this.alterationPanel.add(this.createCheckBoxForm(this.alterationCheckboxes, 'alteration-types-chk-group'));

		// re-draw
		this.doLayout();
	}

});

/**
 * This object represent the whole Survival Analysis Array CGH View
 * @type {*|Object}
 */
var SurvivalAnalysisACGHView = Ext.extend(GenericAnalysisView, {

	// input panel
	inputBar : null,

	// intermediate result panel
	intermediateResultPanel : null,

	// plot curve panel
	plotCurvePanel : null,

	constructor: function () {
		this.init();
	},

	init: function() {

		// first of all, let's reset all major components
		this.resetAll();

		// start drawing  input panel
		this.inputBar = new SurvivalAnalysisInputBar({
			title: 'Input Parameters',
			iconCls: 'newbutton',
			renderTo: 'analysisContainer',
			bbar: this.createToolBar(saInputPanelBtnList)
		});
	},

	resetAll: function () {
		Ext.destroy(this.inputBar);
		Ext.destroy(this.intermediateResultPanel);
		Ext.destroy(this.plotCurvePanel);
	},

	resetResult: function () {
		Ext.destroy(this.intermediateResultPanel);
		Ext.destroy(this.plotCurvePanel);
	},

	createToolBar: function(btnList) {
		return new GenericAnalysisToolBar({
			items: btnList
		});
	},

	submitSurvivalAnalysisaCGHJob: function() {
		 if (this.validateInputs()) {
			console.log('LOG: submit survival analysis acgh job');

			 var params = {
				 url:'url',
				 method:'POST',
				 timeout:'180000',
				 analysis:'SurvivalAnalysisArrayCGH',
				 inputs: [] //TODO
			 };

			this.resetResult();

			var job = this.createJob(params);

			 if (job) {
				 this.runJob(params, this.generateResultGrid, this);
			 }

		 }
	},

	validateInputs: function () {

		var isValid = true;
		var invalidInputs = [];

		var regionVal;
		var survivalAnalysisVal;
		var censoringVal;
		var alterationValues;

		// check if region panel is empty
		if (this.inputBar.regionPanel.isEmpty()) {
			invalidInputs.push(this.inputBar.regionPanel.title);
			isValid = false;
		} else {
			regionVal =  this.inputBar.regionPanel.getInputValue();
		}

		// check if survival time  panel is empty
		if (this.inputBar.survivalPanel.isEmpty()) {
			invalidInputs.push(this.inputBar.survivalPanel.title);
			isValid = false;
		} else {
			survivalAnalysisVal =  this.inputBar.survivalPanel.getInputValue();
		}

		// check censoring variable is empty (e.g status is dead or alive)
		if (this.inputBar.censoringPanel.isEmpty()) {
			invalidInputs.push(this.inputBar.censoringPanel.title);
			isValid = false;
		} else {
			censoringVal =  this.inputBar.censoringPanel.getInputValue();
		}

		//check if alteration values has been selected
		var alterationChkGroup = this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
		alterationValues =  alterationChkGroup.getXValues();
		if (alterationValues.length < 1) {
			isValid = false;
			invalidInputs.push(this.inputBar.alterationPanel.title);
		}

		if (!isValid) {
			var strErrMsg = 'Following needs to be defined: ';
			invalidInputs.each(function (item) {
				strErrMsg += '['+item + '] ';
			})

			// inform user on mandatory inputs need to be defined
			Ext.MessageBox.show({
				title: 'Missing mandatory inputs',
			//	msg: 'Following needs to be defined: ' + invalidInputs,
				msg: strErrMsg,
				buttons: Ext.MessageBox.OK,
				icon: Ext.MessageBox.ERROR
			});

		}

		return isValid;
	},

	/**
	 * generates intermediate result in grid panel
	 * @param data
	 */
	generateResultGrid: function (data, view) {

		data = dummyData; //TODO get JSON data from backend

		Ext.grid.intermediateResultData = data;

		var gridReader = new Ext.data.ArrayReader({}, [
			{name: 'region'},
			{name: 'cytoband'},
			{name: 'pvalue', type: 'float'},
			{name: 'fdr', type: 'float'},
			{name: 'alteration'}
		]);

		var groupStore =  new Ext.data.GroupingStore({
			reader: gridReader,
			data: Ext.grid.intermediateResultData,
			sortInfo:{field: 'region', direction: "ASC"},
			groupField:'alteration'
		});

		var columns =  [
			{id:'region',header: "Region", width: 60, sortable: true, dataIndex: 'region'},
			{header: "Cytoband", width: 20, sortable: true, dataIndex: 'cytoband'},
			{header: "p-value", width: 20, sortable: true, dataIndex: 'pvalue'},
			{header: "fdr", width: 20, sortable: true, dataIndex: 'fdr'},
			{header: "Alteration", width: 20, sortable: true, dataIndex: 'alteration'}
		];

		view.intermediateResultPanel = new GenericAnalysisResultGrid({
			id: 'intermediateGridPanel',
			title: 'Intermediate Result',
			renderTo: 'intermediateResultWrapper',
			store: groupStore,
			bbar: view.createToolBar(saIntermediatePanelBtnList),
			columns: columns
		});

	},

	/**
	 * Get selected rows from the grid
	 * @returns {*}
	 */
	getSelectedRows: function () {
		return this.intermediateResultPanel.getSelectionModel().getSelections();
	},

	/**
	 * Display selected rows in tab panel
	 * @param selectedRegions
	 */
	displaySurvivalPlot: function (selectedRegions) {

		// creating tab panel
		if (this.plotCurvePanel == null) {
			this.plotCurvePanel =  new GenericTabPlotPanel({
				id: 'plotResultCurve',
				renderTo: 'plotResultWrapper'
			});
		}

		// create tab as many as selected rows
		for (var i = 0; i < selectedRegions.length ; i++) {

			// compose tab_id form region name + cytoband + alteration type
			var tab_id = selectedRegions[i].data.region + '_' + selectedRegions[i].data.cytoband + '_' +
				selectedRegions[i].data.alteration;
			tab_id = tab_id.replace(/\s/g,'');   // remove whitespaces

			// Getting the template as blue print for survival curve plot.
			// Template is defined in SurvivalAnalysisaCGH.gsp
			var survivalPlotTpl = Ext.Template.from('template-survival-plot');

			// generate id for download btn
			var survivalDownloadBtn = 'survivalDownloadBtn_' +  tab_id;

			// create data instance
			var region = {
				region:  selectedRegions[i].data.region,
				cytoband:  selectedRegions[i].data.cytoband,
				pvalue:  selectedRegions[i].data.pvalue,
				fdr:  selectedRegions[i].data.fdr,
				survivalDownloadBtn: survivalDownloadBtn,
				alteration:  selectedRegions[i].data.alteration,
				foldername: 'guest-SurvivalAnalysis-102086',  // TODO: get dynamic path to the analysis result
				filename: 'SurvivalCurve2.png' // TODO: get image from analysis result
			};

			// add tab to the container
			this.plotCurvePanel.addTab(region, tab_id, survivalPlotTpl);

			// create export button
			var exportBtn = new Ext.Button ({
				text : 'Download Survival Plot',
				iconCls : 'downloadbutton',
				renderTo: survivalDownloadBtn
			});
		}

	}

});

/**
 *
 * This function is called when user selects Survival Analysis aCGH from the Analysis combo box in the Dataset
 * Explorer toolbar
 *
 */
function loadSurvivalAnalysisaCGHView() {
	// everything starts here ..
	survivalAnalysisACGHView = new SurvivalAnalysisACGHView();
}