/**
 * User: riza
 * Date: 25-04-13
 * Time: 15:54
 */

/**
 * Individual Panel in Input Bar
 * @type {*|Object}
 */
var InputPanel = Ext.extend(Ext.Panel, {

	columnWidth: .25,
	height: 120,
	layout: 'fit',
	notifyFunc: null,
	toolTipTxt: null,

	isDroppable: false, // by default panel is not droppable

	constructor: function(config) {
		InputPanel.superclass.constructor.apply(this, arguments);
	},

	listeners: {
		afterLayout : function () { // is triggered when component has been rendered
			if (this.isDroppable) { // apply droppable when it is
				this.applyDroppable(this.notifyFunc);
			}
			// apply tool tip
			if (this.toolTipTxt != null) {
				this.applyToolTip(this.toolTipTxt);
			}
		}
	},

	// applying droppable
	applyDroppable: function(notifyFunc) {
		var dropTarget = this.getEl().select('.x-panel-bwrap .x-panel-body'); //return array
		var ddTarget = new Ext.dd.DropTarget(dropTarget.elements[0], {
				ddGroup : 'makeQuery',
				notifyDrop : notifyFunc
			}
		);
	},


	applyToolTip: function(html) {
		var ttip = new Ext.ToolTip({
			target: this.getEl(),
			html: html
		});
	}
});

/**
 * Input Bar
 * @type {*|Object}
 */
var InputBar = Ext.extend(Ext.Panel, {

	layout:'column',
	collapsible: true,

	constructor: function(config) {
		InputBar.superclass.constructor.apply(this, arguments);
	},

	init: function () {

	},

	createChildPanel : function(config) {

		var childPanel =  new InputPanel({
			title: config.title,
			id: config.id,
			isDroppable: config.isDroppable,
			notifyFunc: config.notifyFunc,
			toolTipTxt: config.toolTipTxt
		});

		var _this = this;

		//add tool button if it is droppable
		if (config.isDroppable) {
			childPanel.tools = [{
				id: 'refresh',
				handler: function(e, toolEl, panel, tc){
					_this.clearInput(panel.getId());
				}
			}];
		}

		// add childPanel to input panel
		this.add(childPanel);
		return childPanel;
	},

	createCheckBoxForm: function (checkboxes) {
		// define alteration types
		var alterationTypes = new Ext.form.CheckboxGroup({
			// Put all controls in a single column with width 75%
			columns: 1,
			height: 75,
			style: 'margin-left: 5px;',
			items: checkboxes
		});

		return alterationTypes;
	},

	clearInput: function (panelId) {
		// get panel's body div array
		var divEl = Ext.get(panelId).select('.x-panel-bwrap .x-panel-body'); // returns array
		// get panel's body div
		var divName = divEl.elements[0];

		// Clear the drag and drop div.
		var qc = Ext.get(divName);

		for ( var i = qc.dom.childNodes.length - 1; i >= 0; i--)
		{
			var child = qc.dom.childNodes[i];
			qc.dom.removeChild(child);
		}
		clearHighDimDataSelections(divName);
		clearSummaryDisplay(divName);
	}

});

/**
 *
 * @type {*|Object}
 */
var GenericToolBar = Ext.extend(Ext.Toolbar, {
	height: 30,
	constructor: function(config) {
		InputBar.superclass.constructor.apply(this, arguments);
	}
});

/**
 *
 * @type {*|Object}
 */
var ResultGridPanel = Ext.extend(Ext.grid.GridPanel, {

	id: 'intermediateGridPanel',

	columns: [
		{id:'region',header: "Region", width: 60, sortable: true, dataIndex: 'region'},
		{header: "Cytoband", width: 20, sortable: true, dataIndex: 'cytoband'},
		{header: "p-value", width: 20, sortable: true, dataIndex: 'pvalue'},
		{header: "fdr", width: 20, sortable: true, dataIndex: 'fdr'},
		{header: "Alteration", width: 20, sortable: true, dataIndex: 'alteration'}
	],

	view: new Ext.grid.GroupingView({
		forceFit:true,
		groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
	}),

	bbar: null,
	frame:true,
	height: 250,
	collapsible: true,
	animCollapse: false,
	title: 'Intermediate Result',
	iconCls: 'gridbutton',
	renderTo: 'intermediateResultWrapper',

	constructor: function(config) {

		ResultGridPanel.superclass.constructor.apply(this, arguments);

	}

});


var GenericPlotPanel = Ext.extend(Ext.Panel, {
	constructor: function () {
		GenericPlotPanel.superclass.constructor.apply(this, arguments);
	}
});

