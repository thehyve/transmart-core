<html>
<head>
    <title>Transmart Login</title>

    <asset:link href='searchtool.ico' rel="shortcut icon" />
    <asset:link href='searchtool.ico' rel="icon" />


    <asset:javascript src="jquery-plugin.js"/>
    <asset:stylesheet href="extjs.css"/>
    <asset:javascript src="extjs.min.js"/>
    <asset:stylesheet href="main.css"/>
    <script type="text/javascript" charset="utf-8">

        Ext.BLANK_IMAGE_URL = "${resource(dir:'images', file:'s.gif')}";
        Ext.Ajax.timeout = 180000;
        Ext.QuickTips.init();

        var $j = window.$j = jQuery.noConflict();

    </script>

    <g:layoutHead/>
</head>

<body>

<g:layoutBody/>
</body>
</html>
