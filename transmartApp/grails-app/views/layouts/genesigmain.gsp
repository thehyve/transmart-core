<html>
<head>
    <title><g:layoutTitle default="Gene Signature"/></title>

    <asset:link rel="shortcut icon" href='searchtool.ico' type="image/x-ico" />
    <asset:link rel="icon" href='searchtool.ico' type="image/x-ico" />
    %{--<r:require module="signatureTab" />--}%
    <asset:javascript src="jquery-2.2.0.min.js"/>
    <asset:javascript src="jquery-ui.min.js"/>
    <asset:javascript src="session_timeout_nodep.js"/>
    <asset:javascript src="jquery-2.2.0.min.js"/>
    <asset:javascript src="extjs.js"/>
    <asset:javascript src="ext/ext-base.js"/>
    <asset:javascript src="ext/ext-all-debug.js"/>
    <asset:javascript src="ext-ux/miframe.js"/>
    <asset:javascript src="maintabpanel.js"/>
    <asset:javascript src="toggle.js"/>
    <asset:javascript src="ext-ux/miframe.js"/>
    <asset:stylesheet href="extjs.css"/>
    <asset:stylesheet href="main.css"/>
    <asset:stylesheet href="sanofi.css"/>
    <asset:stylesheet href="genesignature.css"/>
    <asset:stylesheet href="jquery-plugin.css"/>

    <script type="text/javascript" charset="utf-8">
        Ext.BLANK_IMAGE_URL = "assets/images/default/s.gif";
        Ext.Ajax.timeout = 180000;
        Ext.onReady(function () {
            Ext.QuickTips.init()
        });

        var $j = window.$j = jQuery.noConflict();

    </script>
</head>

<body>
<div id="page">
    <div id="header"><g:render template="/layouts/commonheader" model="['app': 'genesignature']"/></div>

    <div id="app"><g:layoutBody/></div>
</div>
</body>
</html>
