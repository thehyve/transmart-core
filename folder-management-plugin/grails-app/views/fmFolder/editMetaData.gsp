<!DOCTYPE html>
<html>
    <head>
        <!-- Force Internet Explorer 8 to override compatibility mode -->
        <meta http-equiv="X-UA-Compatible" content="IE=edge" >        
        
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
        <title>${grailsApplication.config.com.recomdata.appTitle}</title>
        
        <!-- jQuery CSS for cupertino theme -->
        <asset:stylesheet href='jquery/ui/jquery-ui-1.9.1.custom.css'/>
        <asset:stylesheet href="jquery/skin/ui.dynatree.css"/>
        
        <!-- Our CSS -->
        <asset:stylesheet href='jquery.loadmask.css'/>
        <asset:stylesheet href='main.css'/>
        <asset:stylesheet href='rwg.css'/>
        <asset:stylesheet href='folderManagement.css'/>
        <asset:stylesheet href='colorbox.css'/>
        <asset:stylesheet href='jquery/simpleModal.css'/>
        <asset:stylesheet href='jquery/multiselect/ui.multiselect.css'/>
        <asset:stylesheet href='jquery/multiselect/common.css'/>
        <asset:stylesheet href='jquery/jqueryDatatable.css'/>
                                
        <!-- jQuery JS libraries -->
        <asset:javascript src="jQuery.min.js"/>
        <script>jQuery.noConflict();</script> 
        
        <asset:javascript src='jQuery/jquery-ui-1.9.1.custom.min.js'/>
        
        <asset:javascript src='jQuery/jquery.cookie.js'/>
        <asset:javascript src='jQuery/jquery.dynatree.min.js'/>
        <asset:javascript src='jQuery/jquery.paging.min.js'/>
        <asset:javascript src='jQuery/jquery.loadmask.min.js'/>
        <asset:javascript src='jQuery/jquery.ajaxmanager.js'/>
        <asset:javascript src='jQuery/jquery.numeric.js'/>
        <asset:javascript src='jQuery/jquery.colorbox-min.js'/>
        <asset:javascript src='jQuery/jquery.simplemodal.min.js'/>
        <asset:javascript src='jQuery/jquery.dataTables.js'/>
        <asset:javascript src='facetedSearch/facetedSearchBrowse.js'/>
        <asset:javascript src='jQuery/ui.multiselect.js'/>
          
                
        <!--Datatable styling and scripts-->
        <asset:javascript src='jquery.dataTables.min.js'/>
        <asset:javascript src='ColVis.min.js'/>
                
        <!--  SVG Export -->
        <%--<asset:javascript src='svgExport/rgbcolor.js'/>  --%>
          
    
        <g:javascript library="prototype" /> 
        <script type="text/javascript">
            var $j = jQuery.noConflict();
        </script>
        
        <!-- Our JS -->        
        <asset:javascript src='rwg.js'/>
        <asset:javascript src='maintabpanel.js'/>
        <asset:javascript src='datasetExplorer.js'/>
        
        <!-- Protovis Visualization library and IE plugin (for lack of SVG support in IE8 -->
        <%-- <asset:javascript src='protovis-r3.2.js'/>
        <asset:javascript src='protovis-msie.min.js'/> --%>
</head>
<body>
<div style="width:800px">
<g:render template="editMetaData" plugin="folderManagement" model="[folder:folder, measurements:measurements, technologies:technologies, vendors:vendors, platforms:platforms, layout:layout]" />
</div>
</body>
</html>
