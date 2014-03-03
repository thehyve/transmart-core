// tranSMART related utils

/**
 * Display error as popup
 * @param title
 * @param msg
 */
function displayError (title, msg) {
    Ext.Msg.show(
        {
            title: title,
            msg: msg,
            buttons: Ext.Msg.OK,
            fn: function () {
                Ext.Msg.hide();
            },
            icon: Ext.MessageBox.ERROR
        }
    );
}

/**
 * Check if a node is high dimensional or not
 * @param node
 * @returns {boolean}
 */
function isHighDimensionalNode (node) {
    if (node.attributes.visualattributes.indexOf('HIGH_DIMENSIONAL') != -1)
        return true;
    else
        return false;
}

/**
 * forEach is a recent addition to the ECMA-262 standard; as such it may not be present in other implementations of the
 * standard. You can work around this by inserting the following code at the beginning of your scripts, allowing use of
 * forEach in implementations which do not natively support it.
 *
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/forEach
 */

if (!Array.prototype.forEach) {
    Array.prototype.forEach = function (fn, scope) {
        'use strict';
        var i, len;
        for (i = 0, len = this.length; i < len; ++i) {
            if (i in this) {
                fn.call(scope, this[i], i, this);
            }
        }
    };
}
