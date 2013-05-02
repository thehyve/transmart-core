/**
 * User: riza
 * Date: 25-04-13
 * Time: 15:54
 */


/**
 * Override CheckboxGroup to have getValues
 */
Ext.override(Ext.form.CheckboxGroup, {
	getNames: function() {
		var n = [];

		this.items.each(function(item) {
			if (item.getValue()) {
				n.push(item.getName());
			}
		});

		return n;
	},

	getValues: function() {
		var v = [];

		this.items.each(function(item) {
			if (item.getValue()) {
				v.push(item.getRawValue());
			}
		});

		return v;
	},

	getXValues: function() {
		var v = [];

		this.items.each(function(item) {
			if (item.getXValue()) {
				v.push(item.getXValue());
			}
		});

		return v;
	},

	setValues: function(v) {
		var r = new RegExp('(' + v.join('|') + ')');

		this.items.each(function(item) {
			item.setValue(r.test(item.getRawValue()));
		});
	}
});

/**
 * Override CheckboxGroup to have getValues
 */
Ext.override(Ext.form.Checkbox, {

	getXValue: function() {
		if (this.getValue()) {
			return this.XValue;
		}
	},

	setXValue: function(xvalue) {
		this.XValue = xvalue;
	}
});

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
	},


	getInputValue: function() {
		return this.getEl().select('.x-panel-bwrap .x-panel-body').item(0);
	},

	isEmpty: function() {
		var isEmpty = true;
		if (this.getInputValue().dom.childNodes.length > 0) {
			isEmpty = false;
		}

		return isEmpty;
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

	createCheckBoxForm: function (checkboxes, id) {
		// define alteration types
		var chkGroup = new Ext.form.CheckboxGroup({
			// Put all controls in a single column with width 75%
			id: id,
			columns: 1,
			height: 75,
			style: 'margin-left: 5px;',
			items: checkboxes
		});

		return chkGroup;
	},

	createRadioBtnGroup: function (radioBtns, id) {
		var radGroup = new Ext.form.RadioGroup({
			id: id,
			columns: 1,
			height: 75,
			vertical: true,
			style: 'margin-left: 5px;',
			items: radioBtns
		});

		return radGroup;
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
 * Generic tool bar
 * @type {*|Object}
 */
var GenericToolBar = Ext.extend(Ext.Toolbar, {
	height: 30,
	constructor: function(config) {
		InputBar.superclass.constructor.apply(this, arguments);
	}
});

/**
 * Generic result grid panel with grouping feature
 * @type {*|Object}
 */
var ResultGridPanel = Ext.extend(Ext.grid.GridPanel, {

	view: new Ext.grid.GroupingView({
		forceFit:true,
		groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
	}),

	bbar: null,
	frame:true,
	height: 250,
	collapsible: true,
	animCollapse: false,
	iconCls: 'gridbutton',

	constructor: function(config) {

		ResultGridPanel.superclass.constructor.apply(this, arguments);

	}

});


/**
 * Panel to display plot of analysis result
 * @type {*|Object}
 */
var GenericPlotPanel = Ext.extend(Ext.Panel, {

	constructor: function () {
		GenericPlotPanel.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function () {

	}

});

/**
 * Tab panel to display survival analysis plots
 * @type {*|Object}
 */
GenericTabPlotPanel = Ext.extend(Ext.TabPanel, {

	width:'100%',
	frame:true,
	height:600,
	defaults: {autoScroll:true},

	templateId: null,

	constructor: function () {
		GenericTabPlotPanel.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function () {

	},

	addTab: function (region, tab_id, templateFile) {

		var p =  this.findById(tab_id); //find if the plot already displayed

		if (p == null) { // if not yet then create and add a tab
			// create tab item
			p = this.add({
				id: tab_id,
				title: region.region,
				closable:true
			});
		}

		//set active tab
		this.setActiveTab(p);

		//redo layout
		this.doLayout();

		// generate template with associated region values in selected tab
		templateFile.overwrite(Ext.get(tab_id), region);

	}


});
