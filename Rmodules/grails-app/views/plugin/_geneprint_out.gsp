
<div id="oncoprint" style="padding-top:10px; padding-bottom:10px; padding-left:10px; border: 1px solid #CCC;">
<img id="outer_loader_img" src="${resource(dir: 'images', file: 'ajax-loader.gif')}" style="display: none;">
<div style="" id="everything">
<h4 style="display:inline;">GenePrint
    <!--TODO: implement: <small>(<a href="faq.jsp#what-are-geneprints">What are GenePrints?</a>)</small>-->
</h4>

<!-- TODO: implement
<span>
    <form id="pdf-form" style="display:inline;" action="svgtopdf.do" method="post">
        <input type="hidden" name="svgelement">
        <input type="hidden" name="filetype" value="pdf">
        <input type="hidden" name="filename" value="oncoprint.pdf">
        <input type="submit" value="PDF">
    </form>

    <form id="svg-form" style="display:inline;" action="oncoprint_converter.svg" enctype="multipart/form-data" method="POST" target="_blank">
        <input type="hidden" name="xml">
        <input type="hidden" name="longest_label_length">
        <input type="hidden" name="format" value="svg">
        <input type="submit" value="SVG">
    </form>
</span>
-->

<div id="oncoprint_controls" style="margin-top:10px; margin-bottom:20px;">
    <style>
    .onco-customize {
        color:#2153AA; font-weight: bold; cursor: pointer;
    }
    .onco-customize:hover { text-decoration: underline; }
    </style>
    <p onclick="$('#oncoprint_controls #main').toggle(); $('#oncoprint_controls .triangle').toggle();" style="margin-bottom: 0px;">
        <span class="triangle ui-icon ui-icon-triangle-1-e" style="float: left; display: block;"></span>
        <span class="triangle ui-icon ui-icon-triangle-1-s" style="float: left; display: none;"></span>
        <span class="onco-customize">Customize</span>
    </p>

    <div id="main" style="display:none;">
        <table style="padding-left:13px; padding-top:5px">
            <tbody><tr>
                <td style="padding-right: 15px;"><span>Zoom</span><div id="zoom" style="display: inline-table;"><div id="width_slider" class="ui-slider ui-slider-horizontal ui-widget ui-widget-content ui-corner-all" aria-disabled="false" style="width: 100px;"><a class="ui-slider-handle ui-state-default ui-corner-all" href="#" style="left: 100%;"></a></div></div></td>
                <td><label><input id="toggle_unaltered_cases" type="checkbox">Remove Unaltered Cases</label></td>
                <td><label><input id="toggle_whitespace" type="checkbox"><label>Remove Whitespace</label></label></td>
            </tr>
            <tr>
                <td>
                    <div id="disable_select_clinical_attributes" style="display: none; z-index: 1000; opacity: 0.7; background-color: grey; width: 22.5%; height: 6%; position: absolute;"></div>
                    <select data-placeholder="add clinical attribute track" id="select_clinical_attributes" style="width: 350px; display: none;" class="chzn-done">
                        <option value=""></option>
                        <option>Agilent_expression</option><option>anatomic_organ_subdivision</option><option>cancer</option><option>copy-number</option><option>Disease Free (Months)</option><option>Disease Free Status</option><option>expression_subtype</option><option>Gender</option><option>histology</option><option>hypermutated</option><option>iCluster</option><option>methylation</option><option>methylation_subtype</option><option>MLH1 silencing</option><option>MSI_status</option><option>Overall Survival (Months)</option><option>Overall Survival Status</option><option>primary_tumor_pathologic_spread</option><option>sequenced</option><option>Site</option><option>tumor_stage</option></select><div id="select_clinical_attributes_chzn" class="chzn-container chzn-container-single" style="width: 240px;" title=""><a href="javascript:void(0)" class="chzn-single chzn-default" tabindex="-1"><span>add clinical attribute track</span><div><b></b></div></a><div class="chzn-drop"><div class="chzn-search"><input type="text" autocomplete="off"></div><ul class="chzn-results"><li id="select_clinical_attributes_chzn_o_1" class="active-result" style="">Agilent_expression</li><li id="select_clinical_attributes_chzn_o_2" class="active-result" style="">anatomic_organ_subdivision</li><li id="select_clinical_attributes_chzn_o_3" class="active-result" style="">cancer</li><li id="select_clinical_attributes_chzn_o_4" class="active-result" style="">copy-number</li><li id="select_clinical_attributes_chzn_o_5" class="active-result" style="">Disease Free (Months)</li><li id="select_clinical_attributes_chzn_o_6" class="active-result" style="">Disease Free Status</li><li id="select_clinical_attributes_chzn_o_7" class="active-result" style="">expression_subtype</li><li id="select_clinical_attributes_chzn_o_8" class="active-result" style="">Gender</li><li id="select_clinical_attributes_chzn_o_9" class="active-result" style="">histology</li><li id="select_clinical_attributes_chzn_o_10" class="active-result" style="">hypermutated</li><li id="select_clinical_attributes_chzn_o_11" class="active-result" style="">iCluster</li><li id="select_clinical_attributes_chzn_o_12" class="active-result" style="">methylation</li><li id="select_clinical_attributes_chzn_o_13" class="active-result" style="">methylation_subtype</li><li id="select_clinical_attributes_chzn_o_14" class="active-result" style="">MLH1 silencing</li><li id="select_clinical_attributes_chzn_o_15" class="active-result" style="">MSI_status</li><li id="select_clinical_attributes_chzn_o_16" class="active-result" style="">Overall Survival (Months)</li><li id="select_clinical_attributes_chzn_o_17" class="active-result" style="">Overall Survival Status</li><li id="select_clinical_attributes_chzn_o_18" class="active-result" style="">primary_tumor_pathologic_spread</li><li id="select_clinical_attributes_chzn_o_19" class="active-result" style="">sequenced</li><li id="select_clinical_attributes_chzn_o_20" class="active-result" style="">Site</li><li id="select_clinical_attributes_chzn_o_21" class="active-result" style="">tumor_stage</li></ul></div></div>
                </td>
                <td>
                    <span>Sort by: </span>
                    <select id="sort_by" style="width: 200px; display: none;" class="chzn-done">
                        <option value="genes">gene data</option>
                        <option value="clinical" disabled="">clinical data</option>
                        <option value="alphabetical">alphabetically by case id</option>
                        <option value="custom">user-defined case list / default</option>
                    </select><div id="sort_by_chzn" class="chzn-container chzn-container-single chzn-container-single-nosearch" style="width: 240px;" title=""><a href="javascript:void(0)" class="chzn-single" tabindex="-1"><span>gene data</span><div><b></b></div></a><div class="chzn-drop"><div class="chzn-search"><input type="text" autocomplete="off"></div><ul class="chzn-results"><li id="sort_by_chzn_o_0" class="active-result result-selected" style="">gene data</li><li id="sort_by_chzn_o_2" class="active-result" style="">alphabetically by case id</li><li id="sort_by_chzn_o_3" class="active-result" style="">user-defined case list / default</li></ul></div></div>
                </td>
            </tr>
            </tbody></table>
    </div>
</div>
<script type="text/template" id="main-controls-template">
    <style>
        .onco-customize {
        color:#2153AA; font-weight: bold; cursor: pointer;
        }
        .onco-customize:hover { text-decoration: underline; }
    </style>
    <p onclick="$('#oncoprint_controls #main').toggle(); $('#oncoprint_controls .triangle').toggle();"
    style="margin-bottom: 0px;">
    <span class="triangle ui-icon ui-icon-triangle-1-e" style="float: left; display: block;"></span>
        <span class="triangle ui-icon ui-icon-triangle-1-s" style="float: left; display: none;"></span>
        <span class='onco-customize'>Customize</span>
    </p>

    <div id="main" style="display:none;">
    <table style="padding-left:13px; padding-top:5px">
    <tr>
                <td style="padding-right: 15px;"><span>Zoom</span><div id="zoom" style="display: inline-table;"></div></td>
                <td><label><input id='toggle_unaltered_cases' type='checkbox'>Remove Unaltered Cases</label></td>
                <td><label><input id='toggle_whitespace' type='checkbox'><label>Remove Whitespace</label></td>
            </tr>
            <tr>
                <td>
                    <div id="disable_select_clinical_attributes" style="display: none; z-index: 1000; opacity: 0.7; background-color: grey; width: 22.5%; height: 6%; position: absolute;"></div>
                    <select data-placeholder="add clinical attribute track" id="select_clinical_attributes" style="width: 350px;">
    <option value=""></option>
                    </select>
                </td>
                <td>
                    <span>Sort by: </span>
                    <select id="sort_by" style="width: 200px;">
    <option value="genes">gene data</option>
                        <option value="clinical" disabled>clinical data</option>
                        <option value="alphabetical">alphabetically by case id</option>
                        <option value="custom">user-defined case list / default</option>
                    </select>
                </td>
            </tr>
        </table>
    </div>
</script>

<script type="text/template" id="custom-controls-template">
    <style>
        .onco-customize {
        color:#2153AA; font-weight: bold; cursor: pointer;
    }
    .onco-customize:hover { text-decoration: underline; }
    </style>
    <p onclick="$('#oncoprint_controls #main').toggle(); $('#oncoprint_controls .triangle').toggle();"
       style="margin-bottom: 0px;">
        <span class="triangle ui-icon ui-icon-triangle-1-e" style="float: left; display: block;"></span>
        <span class="triangle ui-icon ui-icon-triangle-1-s" style="float: left; display: none;"></span>
        <span class='onco-customize'>Customize</span>
    </p>

    <div id="main" style="display:none;">
        <table style="padding-left:13px; padding-top:5px">
            <tr>
                <td style="padding-right: 15px;"><span>Zoom&nbsp;</span><div id="zoom" style="display: inline-table;"></div></td>
            </tr>
            <tr>
                <td><label><input id='toggle_unaltered_cases' type='checkbox'>Remove Unaltered Cases</label></td>
            </tr>
            <tr>
                <td><label><input id='toggle_whitespace' type='checkbox'>Remove Whitespace</label></td>
            </tr>
            <tr>
                <td><label><input id='all_cna_levels' type='checkbox'>Show All CNA levels</label></td>
            </tr>
        </table>
    </div>
</script>


<img id="inner_loader_img" src="${resource(dir: 'images', file: 'ajax-loader.gif')}" style="display:none;">

<div id="oncoprint_body"></div>

<div id="oncoprint_legend" style="padding-left: 102px;"></div>

<script type="text/template" id="glyph_template">
    <svg height="23" width="6">
        <rect fill="{{bg_color}}" width="5.5" height="23"></rect>

        <rect display="{{display_mutation}}" fill="#008000" y="7.666666666666667" width="5.5" height="7.666666666666667"></rect>
        <path display="{{display_fusion}}" d="M0,0L0,23 5.5,11.5Z"></path>

        <path display="{{display_down_rppa}}" d="M0,2.672958956142353L3.0864671457232173,-2.672958956142353 -3.0864671457232173,-2.672958956142353Z" transform="translate(2.75,20.909090909090907)"></path>
        <path display="{{display_up_rppa}}" d="M0,-2.672958956142353L3.0864671457232173,2.672958956142353 -3.0864671457232173,2.672958956142353Z" transform="translate(2.75,2.3000000000000003)"></path>

        <rect display="{{display_down_mrna}}" height="23" width="5.5" stroke-width="2" stroke-opacity="1" stroke="#6699CC" fill="none"></rect>
        <rect display="{{display_up_mrna}}" height="23" width="5.5" stroke-width="2" stroke-opacity="1" stroke="#AA0000" fill="none"></rect>
    </svg>
    <span style="position: relative; bottom: 6px;">{{text}}</span>
</script>

</div>
</div>
