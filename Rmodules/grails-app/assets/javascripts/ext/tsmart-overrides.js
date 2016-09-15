/**
 * Created with IntelliJ IDEA.
 * User: riza
 * Date: 07-05-13
 * Time: 11:05
 * To change this template use File | Settings | File Templates.
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
 *
 */
Ext.override(Ext.form.RadioGroup, {
	getSelectedValue: function() {
		var v;
		this.items.each(function(item) {
			if (item.getValue()) {
				v = item.XValue;
			}
		});
		return v;
	}
});

/**
 * Override CheckboxGroup to have XValue getter & setter
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
 * Override Radio to have XValue getter & setter
 */
Ext.override(Ext.form.Radio, {

	getXValue: function() {
		if (this.getValue()) {
			return this.XValue;
		}
	},

	setXValue: function(xvalue) {
		this.XValue = xvalue;
	}
});


