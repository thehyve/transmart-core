<html>
<head>
    <title>Transmart Login</title>

    <asset:link href='searchtool.ico' rel="shortcut icon" />
    <asset:link href='searchtool.ico' rel="icon" />


    %{--<g:javascript library="jquery" />--}%
    <asset:javascript src="jquery-2.2.0.min.js"/>
    <asset:stylesheet href="extjs.css"/>
    <asset:javascript src="extjs.js"/>
    %{--<r:require module="extjs" />--}%
    %{--<r:layoutResources/>--}%
    <asset:stylesheet href='main.css'/>
    <script type="text/javascript" charset="utf-8">

        Ext.BLANK_IMAGE_URL = "${resource(dir:'images', file:'s.gif')}";
        Ext.Ajax.timeout = 180000;
        Ext.QuickTips.init();

        var $j = window.$j = jQuery.noConflict();

    </script>

    <g:layoutHead/>
    %{--<r:layoutResources/>--}%
</head>

<body>

<g:layoutBody/>
%{--<asset:deferredScripts/>--}%
%{--<r:layoutResources/>--}%
</body>
</html>
