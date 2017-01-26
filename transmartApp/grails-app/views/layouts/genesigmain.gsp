<html>
<head>
    <title><g:layoutTitle default="Gene Signature"/></title>

    <asset:link rel="shortcut icon" href='searchtool.ico' type="image/x-ico" />
    <asset:link rel="icon" href='searchtool.ico' type="image/x-ico" />
    <asset:javascript src="jquery-plugin.js"/>
    <asset:javascript src="session_timeout_nodep.js"/>
    <asset:javascript src="extjs.min.js"/>
    <asset:javascript src="maintabpanel.js"/>
    <asset:javascript src="toggle.js"/>
    <asset:stylesheet href="extjs.css"/>
    <asset:stylesheet href="main.css"/>
    <asset:stylesheet href="sanofi.css"/>
    <asset:stylesheet href="genesignature.css"/>
    <asset:stylesheet href="jquery-plugin.css"/>

    <asset:script type="text/javascript" charset="utf-8">
        Ext.BLANK_IMAGE_URL = "${resource(dir:'images', file:'s.gif')}";
        Ext.Ajax.timeout = 180000;
        Ext.onReady(function () {
            Ext.QuickTips.init()
        });

        var $j = window.$j = jQuery.noConflict();
    </asset:script>

    <asset:deferredScripts/>
</head>

<body>
<div id="page">
    <div id="header"><g:render template="/layouts/commonheader" model="['app': 'genesignature']"/></div>

    <div id="app"><g:layoutBody/></div>
</div>
</body>
</html>
