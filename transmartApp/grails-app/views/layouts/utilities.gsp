<html>

    <title><g:layoutTitle default=""/></title>
    <asset:javascript src="jquery-plugin.js"/>
    <asset:javascript src="session_timeout_nodep.js"/>
    <asset:stylesheet src="admintab.css"/>
    <asset:javascript src="admintab.min.js"/>
    <asset:stylesheet src="admin"/>
    <asset:script>
    Ext.BLANK_IMAGE_URL = "${resource(dir:'images', file:'s.gif')}";

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
    <div id="header-div"><g:render template="/layouts/commonheader" model="['app': 'admin']"/></div>

    <div style="float: right; width: 97%;"><g:layoutBody/></div>
</div>
</body>
</html>