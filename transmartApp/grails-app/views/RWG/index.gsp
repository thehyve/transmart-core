<!DOCTYPE HTML>
<html>
<head>
    <!-- Force Internet Explorer 8 to override compatibility mode -->
    <meta http-equiv="X-UA-Compatible" content="IE=Edge">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

    <title>${grailsApplication.config.com.recomdata.appTitle}</title>

<asset:link rel="shortcut icon" href='searchtool.ico' type="image/x-ico" />
<asset:link rel="icon" href='searchtool.ico' type="image/x-ico" />

    <asset:javascript src="jquery-plugin.js"/>
    <tmpl:/RWG/urls/>

<script type="text/javascript" charset="utf-8">
        var mouse_inside_options_div = false;
        window.rwgSearchConfig = {
            requiredField: 'FOLDER_ID',
            onFoldersListChanges: function() { window.rwg_onFoldersListChanges.apply(this, arguments); },
        };

        jQuery(document).ready(function () {

            addToggleButton();

            jQuery("#xtButton").colorbox({opacity: .75, inline: true, width: "95%", height: "95%"});

            jQuery("#searchResultOptions_btn").click(function () {
                jQuery("#searchResultOptions").toggle();
            });

            //used to hide the options div when the mouse is clicked outside of it

            jQuery('#searchResultOptions_holder').hover(function () {
                mouse_inside_options_div = true;
            }, function () {
                mouse_inside_options_div = false;
            });

            jQuery("body").mouseup(function () {
                //top menu options
                if (!mouse_inside_options_div) {
                    jQuery('#searchResultOptions').hide();
                }

                var analysisID = jQuery('body').data('heatmapControlsID');

                if (analysisID > 1) {
                    jQuery('#heatmapControls_' + analysisID).hide();
                }

            });

            jQuery("#editMetadataOverlay").on('click', '#cancelmetadatabutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#editMetadataOverlay').fadeOut();
            });

            jQuery("#editMetadataOverlay").on('click', '#savemetadatabutton', function () {
                if (jQuery(this).hasClass('buttonloading')) {return false; }

            var serializedForm = jQuery('#editMetadataForm').serialize();
                jQuery('#savemetadatabutton').addClass('buttonloading').html("&nbsp;");

                jQuery.ajax({
                    url: saveMetaDataURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#editMetadataOverlay').scrollTop(0);
                            jQuery('#displayMetadataErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#savemetadatabutton').removeClass('buttonloading').text('Save');
                        } else {
                            jQuery('#result-folder-name-' + response.id).text(response.folderName);
                            jQuery('#editMetadataOverlay').fadeOut();
                            showDetailDialog(response.id);
                        }
                    },
                    error: function (xhr) {
                        jQuery('#savemetadatabutton').removeClass('buttonloading').text('Save');
                        alert(xhr);
                    },
                    type: 'POST'
                });
            });

            jQuery("#createAssayOverlay").on('click', '#cancelassaybutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#createAssayOverlay').fadeOut();
            });

            jQuery("#createAssayOverlay").on('click', '#saveassaybutton', function () {
            var serializedForm = jQuery('#createAssayForm').serialize();
                jQuery('#saveassaybutton').addClass('buttonloading').html("&nbsp;");
                jQuery.ajax({
                    url: saveAssayURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#createAssayOverlay').scrollTop(0);
                            jQuery('#displayAssayErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#saveassaybutton').removeClass('buttonloading').text('Save');
                        } else {
                            jQuery('#createAssayOverlay').fadeOut();
                            updateForNewFolder(response.id);
                        }
                    },
                    error: function (xhr) {
                        alert(xhr);
                        jQuery('#saveassaybutton').removeClass('buttonloading').text('Save');
                    },
                    type: 'POST'
                });
            });


            jQuery("#createFolderOverlay").on('click', '#cancelfolderbutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#createFolderOverlay').fadeOut();
            });

            jQuery("#createFolderOverlay").on('click', '#savefolderbutton', function () {
            var serializedForm = jQuery('#createFolderForm').serialize();
                jQuery('#savefolderbutton').addClass('buttonloading').html("&nbsp;");
                jQuery.ajax({
                    url: saveFolderURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#createFolderOverlay').scrollTop(0);
                            jQuery('#displayFolderErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#savefolderbutton').removeClass('buttonloading').text('Save');
                        } else {
                            jQuery('#createFolderOverlay').fadeOut();
                            updateForNewFolder(response.id);
                        }
                    },
                    error: function (xhr) {
                        alert(xhr);
                        jQuery('#savefolderbutton').removeClass('buttonloading').text('Save');
                    },
                    type: 'POST'
                });
            });


            jQuery("#createStudyOverlay").on('click', '#cancelstudybutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#createStudyOverlay').fadeOut();
            });

            jQuery("#createStudyOverlay").on('click', '#savestudybutton', function () {
            var serializedForm = jQuery('#createStudyForm').serialize();
                jQuery('#savestudybutton').addClass('buttonloading').html("&nbsp;");
                jQuery.ajax({
                    url: saveStudyURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#createStudyOverlay').scrollTop(0);
                            jQuery('#displayStudyErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#savestudybutton').removeClass('buttonloading').text('Save');
                        } else {
                            jQuery('#createStudyOverlay').fadeOut();
                            updateForNewFolder(response.id);
                        }
                    },
                    error: function (xhr) {
                        alert(xhr);
                        jQuery('#savestudybutton').removeClass('buttonloading').text('Save');
                    },
                    type: 'POST'
                });
            });

            jQuery("#createProgramOverlay").on('click', '#cancelprogrambutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#createProgramOverlay').fadeOut();
            });

            jQuery("#createProgramOverlay").on('click', '#saveprogrambutton', function () {

            var serializedForm = jQuery('#createProgramForm').serialize();
                jQuery('#saveprogrambutton').addClass('buttonloading').html("&nbsp;");
                jQuery.ajax({
                    url: saveProgramURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#createProgramOverlay').scrollTop(0);
                            jQuery('#displayProgramErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#saveprogrambutton').removeClass('buttonloading').text('Save');
                        } else {
                            showSearchResults();
                            jQuery('#createProgramOverlay').fadeOut();
                            showDetailDialog(response.id);
                        }
                    },
                    error: function (xhr) {
                        alert(xhr);
                        jQuery('#saveprogrambutton').removeClass('buttonloading').text('Save');
                    },
                    type: 'POST'
                });
            });

            jQuery("#createAnalysisOverlay").on('click', '#cancelanalysisbutton', function () {
                if (!confirm('Are you sure you want to cancel your changes?')) {return false;}
                jQuery('#createAnalysisOverlay').fadeOut();
            });

            jQuery("#createAnalysisOverlay").on('click', '#saveanalysisbutton', function () {

            var serializedForm = jQuery('#createAnalysisForm').serialize();
                jQuery('#saveanalysisbutton').addClass('buttonloading').html("&nbsp;");
                jQuery.ajax({
                    url: saveAnalysisURL + "?" + serializedForm,
                    success: function (response) {
                        if (response.errors != undefined) {
                            jQuery('#createAnalysis').scrollTop(0);
                            jQuery('#displayAnalysisErrors').empty().html('<div class="errors">' + response.errors + '</div>');
                            jQuery('#saveanalysisbutton').removeClass('buttonloading').text('Save');
                        } else {
                            jQuery('#createAnalysisOverlay').fadeOut();
                            updateForNewFolder(response.id);
                        }
                    },
                    error: function (xhr) {
                        alert(xhr);
                        jQuery('#saveanalysisbutton').removeClass('buttonloading').text('Save');

                    },
                    type: 'POST'
                });
            });

            resizeAccordion();

            jQuery('#sidebar').resizable({
                handles: 'e',
                maxWidth: 800,
            minWidth: 280,
                resize: function (event, ui) {
                    var currentWidth = ui.size.width;

                    // this accounts for padding in the panels +
                    // borders, you could calculate this using jQuery
                    var padding = 12;

                    // this accounts for some lag in the ui.size value, if you take this away
                    // you'll get some unstable behaviour
                    jQuery(this).width(currentWidth);

                jQuery('#box-search').width(currentWidth - 15)
                jQuery('#program-explorer').width(currentWidth - 15)
                    // jQuery('#results-div').width(currentWidth -20)
                    // set the content panel width
                    jQuery('#main').width(jQuery('body').width() - currentWidth - padding);
                    jQuery('#filter-browser').css('left', jQuery('#box-search').width() + 50);
                }
            });

        jQuery('#sidebar').trigger("resize");

        });

        jQuery(window).resize(function () {
            resizeAccordion();
        });

        function resizeAccordion() {

            var windowHeight = jQuery(window).height();
            jQuery('#sidebar').height(jQuery(window).height() - 30);
            jQuery('#main').height(jQuery(window).height() - 30);
            var ypos = jQuery('#program-explorer').offset()['top'];

        var targetHeight = windowHeight - ypos - 40;
            jQuery('#results-div').height(targetHeight);
            jQuery('#welcome').height(windowHeight - 90);

            if (jQuery('#sidebar:visible').size() > 0) {
                jQuery('#main').width(jQuery('body').width() - jQuery('#sidebar').width() - 12);
            }
            else {
                jQuery('#main').width("100%");
            }


            jQuery('#box-search').width(jQuery('#program-explorer').width());
        }

        function updateExportCount() {
            var checkboxes = jQuery('#exporttable input:checked');

            if (checkboxes.size() == 0) {
                jQuery('#exportbutton').text('No files to export').addClass('disabled');
            }
            else {
                jQuery('#exportbutton').removeClass('disabled').text('Export selected files (' + checkboxes.size() + ')');
            }
        }

        function dataTableWrapper (containerId, tableId, title, sort, pageSize)
        {

            var data;
            var gridPanelHeaderTips;

            function setupWrapper()
            {
                var gridContainer = $j('#' + containerId);
                gridContainer.html('<table id=\'' + tableId + '\'></table></div>');
            }

            function overrideSort() {

                $j.fn.dataTableExt.oSort['numeric-pre'] = function (a) {

                    var floatA = parseFloat(a);
                    var returnValue;

                    if (isNaN(floatA))
                        returnValue = Number.MAX_VALUE * -1;    //Emptys will go to top for -1, bottom for +1
                    else
                        returnValue = floatA;

                    return returnValue;};
            };

            this.loadData = function (dataIn) {


                setupWrapper();

                data = dataIn;
                setupGridData(data, sort, pageSize);

                gridPanelHeaderTips = data.headerToolTips.slice(0);

                //Add the callback for when the grid is redrawn
                data.fnDrawCallback = function (oSettings) {

                    //Hide the pagination if both directions are disabled.
                    if (jQuery('#' + tableId + '_paginate .paginate_disabled_previous').size() > 0 && jQuery('#' + tableId + '_paginate .paginate_disabled_next').size() > 0) {
                        jQuery('#' + tableId + '_paginate').hide();
                    }
                };

                data.fnInitComplete = function() {this.fnAdjustColumnSizing();};

                $j('#' + tableId).dataTable(data);

                $j(window).bind('resize', function () {
                    if ($j('#' + tableId).dataTable().oSettings) {
                        $j('#' + tableId).dataTable().fnAdjustColumnSizing();
                    }
                });

                $j("#" + containerId + " div.gridTitle").html(data.iTitle);

            };


            function setupGridData(data, sort, pageSize)
            {
                data.bAutoWidth = true;
                data.bScrollAutoCss = true;
//                    data.sScrollY = 400;
                data.sScrollX = "100%";
                data.bDestroy = true;
                data.bProcessing = true;
                data.bLengthChange = false;
                data.bScrollCollapse = false;
                data.iDisplayLength = 10;
                if (pageSize != null && pageSize > 0) {
                    data.iDisplayLength = pageSize;
                }
                if (sort != null) {
                    data.aaSorting = sort;
                }
                data.sDom = '<"top"<"gridTitle">Rrt><"bottom"p>' //WHO DESIGNED THIS
            }
        }


        //		var panel = createOntPanel()
        //		jQuery('#metadata-viewer').empty()
        //           jQuery('#metadata-viewer').add(panel);
</script>

<script type="text/javascript">
        jQuery(function ($) {
            // Load dialog on click of Save link
            $('#save-modal .basic').click(openSaveSearchDialog);
        });
</script>

<asset:javascript src="browsetab.min.js" />
<asset:stylesheet href="browseTab.css" />
<asset:stylesheet href="rwg.css" />
<asset:javascript src="rwgsearch.js" />
</head>
<body>

<div id="header-div" class="header-div">
    <g:render template="/layouts/commonheader" model="['app':'datasetExplorer', 'utilitiesMenu':'true']" />
    %{--<g:render template="/layouts/commonheader" model="['app': 'rwg', 'utilitiesMenu': 'true']"/>--}%
</div>

<div id="sidebar" style="width:320px; border-right:5px solid;border-color:#EDEEF6">

    <tmpl:/RWG/boxSearch/>

    <div id="program-explorer">
        <div id="title-program-div" class="ui-widget-header boxtitle">
            <h2 style="float:left" class="title">Program Explorer</h2>
        </div>
        <div id="results-div" class="boxcontent" style="overflow: auto;">
            &nbsp;
        </div>
    </div>

    <div id="filter-div" style="display: none;"></div>

</div>

<div id="main">
    <div id="folder-viewer">
        <div id="welcome-viewer">
            <tmpl:welcome/>
        </div>
        <div id="metadata-viewer">
        </div>

        <div id="subfolder-viewer">
        </div>
    </div>

    <div id="subject-view-div" style="display: none;">

    </div>
    <div id="export-div" style="display: none;">

    </div>
</div>

<div id="hiddenItems" style="display:none">
    <!-- For image export -->
    <canvas id="canvas" width="1000px" height="600px"></canvas>

</div>

<!--  This is the DIV we stuff the browse windows into. -->
<div id="exportOverlay" class="overlay" style="display: none;">&nbsp;</div>
<tmpl:editMetadataOverlay/>
<tmpl:createAnalysisOverlay/>
<tmpl:createAssayOverlay/>
<tmpl:createFolderOverlay/>
<tmpl:createStudyOverlay/>
<tmpl:createProgramOverlay/>
<div id="divBrowsePopups" style="width:800px; display: none;">

</div>

<!--  Note: this div, for the Manhattan Plot options dialog, has been moved to the transmart-gwas plugin -->
<div id="divPlotOptions" style="width:300px; display: none;">
    <table class="columndetail">
        <tr>
            <td class="columnname">SNP Annotation Source</td>
            <td>
                <select id="plotSnpSource" style="width: 220px">
                    <option value="19">Human Genome 19</option>
                    <option value="18">Human Genome 18</option>
                </select>
            </td>
        </tr>
        <%--<tr>
            <td class="columnname">Gene Annotation Source</td>
            <td>
                <select id="plotGeneSource" style="width: 220px">
                    <option id="GRCh37">Human Gene data from NCBI</option>
                </select>
            </td>
        </tr>--%>
        <tr>
            <td class="columnname">P-value cutoff</td>
            <td>
                <input id="plotPvalueCutoff" style="width: 210px">
            </td>
        </tr>
    </table>
</div>

<!-- Everything for the across trial function goes here and is displayed using colorbox -->
<div style="display:none">
    <div id="xtHolder">
        <div id="xtTopbar">
            <p>Cross Trial Analysis</p>
            <ul id="xtMenu">
                <li>Summary</li>
                <li>Heatmap</li>
                <li>Boxplot</li>
            </ul>
            <p>close</p>
        </div>
        <div id="xtSummary"><!-- Summary Tab Content --></div>
        <div id="xtHeatmap"><!-- Heatmap Tab Content --></div>
        <div id="xtBoxplot"><!-- Boxplot Tab Content --></div>
    </div>
</div>

<%-- Elements that are in fixed positions on the page --%>
<div id="sidebartoggle" class="left-arrow">&nbsp;</div>
<tmpl:/RWG/filterBrowser/>

<!--  Used to measure the width of a text element (in svg plots) -->
<span id="ruler" style="visibility: hidden; white-space: nowrap;"></span>
<div id="testTextHeightDiv"></div>
%{--<r:layoutResources/><%-- XXX: Use template --%>--}%
</body>
</html>
