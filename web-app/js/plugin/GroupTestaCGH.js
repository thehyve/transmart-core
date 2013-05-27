/**
 * User: riza
 * Date: 23-04-13
 * Time: 11:28
 */

var groupTestView;

/**
 * Buttons for Input Panel
 * @type {Array}
 */
var gtInputBarBtnList = ['->',{  // '->' making it right aligned
	xtype: 'button',
	text: 'Run Analysis',
	scale: 'medium',
	iconCls: 'runbutton',
	handler: function () {
		groupTestView.submitGroupTestJob();
	}
}];

var GroupTestInputWidget = Ext.extend(GenericAnalysisInputBar, {

	regionPanel: null,
	groupPanel: null,
	statTestPanel: null,
	alterationPanel: null,

	statRadios: [
		{boxLabel: 'Chi-square', name: 'stat-test-opt', XValue:'Chi-square'},
		{boxLabel: 'Wilcoxon', name: 'stat-test-opt', XValue:'Wilcoxon'},
		{boxLabel: 'Kruskal-Wallis', name: 'stat-test-opt', XValue:'KW'}
	],

	alterationRadios: [
		{boxLabel: 'GAIN vs NO GAIN', name: 'cb-col', XValue:'1'},
		{boxLabel: 'LOSS vs NO LOSS', name: 'cb-col', XValue:'-1'},
		{boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'cb-col', XValue:'0'}
	],

	constructor: function(config) {
		GroupTestInputWidget.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function() {

		// define child panel configs
		var childPanelConfig = [{
			title: 'Region',
			id:  'gt-input-region',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Region'

		},{
			title: 'Group',
			id:  'gt-input-group',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Group'

		},{
			title: 'Statistical Test',
			id: 'gt-input-stat-test',
			toolTipTxt: 'Tool-tip for Statistical Test'
		},{
			title: 'Alteration Type',
			id:  'gt-input-alteration',
			toolTipTxt: 'Tool-tip for Alteration Type'
		}];

		// create child panels
		this.regionPanel = this.createChildPanel(childPanelConfig[0]);
		this.groupPanel = this.createChildPanel(childPanelConfig[1]);
		this.statTestPanel = this.createChildPanel(childPanelConfig[2]);
		this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

		// create check boxes
		this.statTestPanel.add(this.createRadioBtnGroup(this.statRadios,'stat-test-chk-group'));
		this.alterationPanel.add(this.createRadioBtnGroup(this.alterationRadios,'alteration-types-chk-group'));

		// re-draw
		this.doLayout();
	}
});


/**
 * This class represents the whole Group Test view
 * @type {*|Object}
 */
var GroupTestView = Ext.extend(GenericAnalysisView, {

	// input panel
	inputBar : null,

	// result panel
	resultPanel : null,

	// constructor
	constructor: function() {
		this.init();
	},

	init: function() {

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

	resetAll: function () {
		this.tabIndex = 0;
		Ext.destroy(this.inputBar);
		Ext.destroy(this.resultPanel);
	},


	createInputToolBar: function() {
		var _this = this;
		return new Ext.Toolbar({
			height: 30,
			items: gtInputBarBtnList
		});
	},

	areAllMandatoryFieldsFilled: function() {
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
		statisticalVal =  statChkGroup.getXValues();
		if (statisticalVal.length < 1) {
			isValid = false;
			invalidInputs.push(this.inputBar.statTestPanel.title);
		}

		//check if alteration values has been selected
		var alterationChkGroup = this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
		alterationVal =  alterationChkGroup.getXValues();
		if (alterationVal.length < 1) {
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
				msg: strErrMsg,
				buttons: Ext.MessageBox.OK,
				icon: Ext.MessageBox.ERROR
			});

		}

		return isValid;
	},

	isGroupFieldValid: function() {
		if (this.inputBar.groupPanel.getConceptCodes().length < 2) {
			Ext.MessageBox.show({
				title: 'Number of groups',
				msg: 'There should be selected more than one group.',
				buttons: Ext.MessageBox.OK,
				icon: Ext.MessageBox.ERROR
			});
			return false;
		}
		return true;
	},

	validateInputs: function() {
		return this.areAllMandatoryFieldsFilled() && this.isGroupFieldValid()
	},

	createResultPlotPanel: function (result) {

		this.resultPanel = new GenericPlotPanel({
			id: 'plotResultCurve',
			renderTo: 'gtPlotWrapper',
			width:'100%',
			frame:true,
			height:600,
			defaults: {autoScroll:true}
		});

		// Getting the template as blue print for survival curve plot.
		// Template is defined in GroupTestaCGH.gsp
		var groupTestPlotTpl = Ext.Template.from('template-group-test-plot');

		// generate template with associated region values in selected tab
		groupTestPlotTpl.overwrite(Ext.get( 'plotResultCurve'));

		// create export button
		var exportBtn = new Ext.Button ({
			text : 'Download Survival Plot',
			iconCls : 'downloadbutton',
			renderTo: 'gtDownload'
		});

	},

    onJobFinish: function() {
	    //FIXME To be able to run test again
	    GLOBAL.CurrentSubsetIDs[1] = null;
	    GLOBAL.CurrentSubsetIDs[2] = null;
    },

	submitGroupTestJob: function () {
		if (this.validateInputs()) {
            var regionVal = this.inputBar.regionPanel.getConceptCode();
            var groupVals = this.inputBar.groupPanel.getConceptCodes();
            var statTestComponent = this.inputBar.statTestPanel.getComponent('stat-test-chk-group');
            var statTestVal =  statTestComponent.getSelectedValue();
            var alternationComponent = this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
            var alternationVal =  alternationComponent.getSelectedValue();

            // compose params
            var formParams = {
	            regionVariable: regionVal,
	            groupVariable: groupVals.join('|'),
	            statisticsType: statTestVal,
	            aberrationType: alternationVal,
                jobType: 'aCGHgroupTest'
            };

            var job = this.submitJob(formParams, this.onJobFinish, this);
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