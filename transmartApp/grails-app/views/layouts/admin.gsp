<html>
<head>
    <title>Transmart Administration</title>

    %{--<g:javascript library="jquery" />--}%
    <asset:javascript src="jquery-2.2.0.min.js"/>
    <asset:stylesheet src="admintab.css"/>
    <asset:javascript src="admintab.js"/>
    %{--<r:require module="adminTab" />--}%

    <asset:script>
			Ext.BLANK_IMAGE_URL = "assets/images/default/s.gif'";

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
    %{--<g:layoutHead/>--}%
    %{--<r:layoutResources/>--}%
</head>

<body>
<div id="page">
    <div id="header-div"><g:render template="/layouts/commonheader" model="['app': 'accesslog']"/></div>

    <div id='navbar'><g:render template="/layouts/adminnavbar"/></div>

    <div id="content"><g:layoutBody/></div>
    %{--<r:layoutResources/>--}%
</div>
</body>
</html>
