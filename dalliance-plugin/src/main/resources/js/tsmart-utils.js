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
