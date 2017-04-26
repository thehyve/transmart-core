<html>
<head>
    <title><g:layoutTitle default=""/></title>
    <asset:stylesheet src="admin"/>
    <asset:javascript src="admin"/>
    <g:setProvider library="jquery"/>
    <asset:script>

        Ext.BLANK_IMAGE_URL = "${resource(dir: 'js', file: 'ext/resources/images/default/s.gif')}";

            // set ajax to 90*1000 milliseconds
            Ext.Ajax.timeout = 180000;
            var pageInfo;

            Ext.onReady(function()
            {
                Ext.QuickTips.init();

                var helpURL = '${grailsApplication.config.com.recomdata.adminHelpURL}';
                var contact = '${grailsApplication.config.com.recomdata.contactUs}';
                var appTitle = '${grailsApplication.config.com.recomdata.appTitle}';
                var buildVer = 'Build Version: <g:meta name="environment.BUILD_NUMBER"/> - <g:meta
            name="environment.BUILD_ID"/>';
                   
                var viewport = new Ext.Viewport({
                    layout: "border",
                    items:[new Ext.Panel({                          
                       region: "center",  
                       //tbar: createUtilitiesMenu(helpURL, contact, appTitle,'${request.getContextPath()}', buildVer, 'admin-utilities-div'),
                       autoScroll:true,                     
                       contentEl: "page"
                    })]
                });
                viewport.doLayout();

                pageInfo = {
                    basePath :"${request.getContextPath()}"
                }
            });

    </asset:script>
    <g:layoutHead/>
</head>

<body>
<div id="page">
    <div id="header-div"><g:render template="/layouts/commonheader"/></div>

    <div style="float: right; width: 97%;"><g:layoutBody/></div>
</div>
</body>
</html>