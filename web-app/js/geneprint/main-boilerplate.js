// boilerplate for the main portal page
//
// Gideon Dresdner July 2013
var MainBoilerplate = function(Oncoprint, utils) {
    var boilerplate = {};

    // This is for the moustache-like templates
    // prevents collisions with JSP tags
    _.templateSettings = {
        interpolate : /\{\{(.+?)\}\}/g
    };

    // add in controls from template
    document.getElementById('oncoprint_controls').innerHTML
        = _.template(document.getElementById('main-controls-template').innerHTML)();

    var clinicalAttributes = new ClinicalAttributesColl();

    var $zoom_el = $('#oncoprint_controls #zoom');
    var zoom;

    // basically a hack to prevent the zoom function from a particular oncoprint
    // from getting bound to the UI slider forever
    var reset_zoom = function() {
        $zoom_el.empty();
        zoom = utils.zoomSetup($zoom_el, oncoprint.zoom);

        return zoom;
    };

    clinicalAttributes.fetch({
        type: 'GET',
        data: { cancer_study_id: cancer_study_id_selected,
            case_list: window.PortalGlobals.getCases() },
        success: function(attrs) {
            utils.populate_clinical_attr_select(document.getElementById('select_clinical_attributes'), attrs.toJSON());
            $(select_clinical_attributes_id).chosen({width: "240px", "font-size": "12px", search_contains: true});
        }
    });

    var oncoprint;

    var cases = [];
    var genes = [];

    var outer_loader_img = $('#oncoprint #outer_loader_img');
    var inner_loader_img = $('#oncoprint #inner_loader_img');

    var geneDataColl = new GeneDataColl();
    boilerplate.xhr = geneDataColl.fetch({
        type: "GET",
        data: window.formParams,
        success: function(data) {
            var geneData = data.toJSON();

            // Collect genes and cases (samples) from the data
            geneData.forEach(function(entry) {
                if (genes.indexOf(entry.gene) == -1) {
                    genes.push(entry.gene);
                }
                if (cases.indexOf(entry.sample) == -1) {
                    cases.push(entry.sample);
                }
            })

            // Set up oncoprint
            oncoprint = Oncoprint(document.getElementById('oncoprint_body'), {
                geneData: geneData,
                genes: genes,
                legend: document.getElementById('oncoprint_legend'),
                width: $('#oncoprint').width() + 50
            });
            outer_loader_img.hide();
            $('#oncoprint #everything').show();

            oncoprint.sortBy(sortBy.val(), cases);

            zoom = reset_zoom();
        },
        error: function(xhr, textStatus, errorThrown) {
            Ext.MessageBox.show({
                title: "Error",
                msg: textStatus.statusText,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });
        },
        complete: function() {
            // Close progress dialog
            $('.runAnalysisBtn')[0].disabled = false;
            destroyWorkflowStatus();
        }
    });

    var select_clinical_attributes_id = '#select_clinical_attributes';
    var oncoprintClinicals;
    var sortBy = $('#oncoprint_controls #sort_by');
    sortBy.chosen({width: "240px", disable_search: true });

    // params: bool
    // enable or disable all the various oncoprint controls
    // true -> enable
    // false -> disable
    var toggleControls = function(bool) {
        var whitespace = $('#toggle_whitespace');
        var unaltered = $('#toggle_unaltered_cases');
        var select_clinical_attributes =  $(select_clinical_attributes_id);

        var enable_disable = !bool;

        whitespace.attr('disabled', enable_disable);
        unaltered.attr('disabled', enable_disable);
        select_clinical_attributes.prop('disabled', enable_disable).trigger("liszt:updated");
        zoom.attr('disabled', enable_disable);
        sortBy.prop('disabled', enable_disable).trigger("liszt:updated");
    };

    // handler for when user selects a clinical attribute to visualization
    var clinicalAttributeSelected = function() {
        oncoprint.remove_oncoprint();
        inner_loader_img.show();
        toggleControls(false);

        var clinicalAttribute = $(select_clinical_attributes_id + ' option:selected')[0].__data__;

        if (clinicalAttribute.attr_id === undefined) {      // selected "none"
            inner_loader_img.hide();

            oncoprint = Oncoprint(document.getElementById('oncoprint_body'), {
                geneData: geneDataColl.toJSON(),
                genes: genes,
                legend: document.getElementById('oncoprint_legend')
            });

            oncoprint.sortBy(sortBy.val(), cases);

            // disable the option to sort by clinical data
            $(sortBy.add('option[value="clinical"]')[1]).prop('disabled', true);
        } else {
            oncoprintClinicals = new ClinicalColl();

            oncoprintClinicals.fetch({
                type: "GET",
                data: {
                    cancer_study_id: cancer_study_id_selected,
                    attribute_id: clinicalAttribute.attr_id,
                    case_list: cases.join(" ")
                },
                success: function(response) {
                    inner_loader_img.hide();

                    oncoprint = Oncoprint(document.getElementById('oncoprint_body'), {
                        geneData: geneDataColl.toJSON(),
                        clinicalData: response.toJSON(),
                        genes: genes,
                        clinical_attrs: response.attributes(),
                        legend: document.getElementById('oncoprint_legend')
                    });

                    oncoprint.sortBy(sortBy.val(), cases);

                    // enable the option to sort by clinical data
                    $(sortBy.add('option[value="clinical"]')[1]).prop('disabled', false);

                    // sort by genes by default
                    sortBy.val('genes');

                    toggleControls(true);

                    zoom = reset_zoom();

                    // sync
                    oncoprint.zoom(zoom.slider("value"));
                    oncoprint.showUnalteredCases(!$('#toggle_unaltered_cases').is(":checked"));
                    oncoprint.toggleWhiteSpace(!$('#toggle_whitespace').is(":checked"));
                    utils.make_mouseover(d3.selectAll('.sample rect'));        // hack =(
                }
            });
        }
    };
    $(select_clinical_attributes_id).change(clinicalAttributeSelected);

    $(document).ready(function() {
        // bind away
        $('#oncoprint_controls #sort_by').change(function() {
            oncoprint.sortBy(sortBy.val(), cases);
        });

        $('#toggle_unaltered_cases').click(function() {
            oncoprint.toggleUnalteredCases();
            utils.make_mouseover(d3.selectAll('.sample rect'));     // hack =(
//            oncoprint.sortBy(sortBy.val());
        });

        $('#toggle_whitespace').click(function() {
            oncoprint.toggleWhiteSpace();
        });

        $('#pdf-form').submit(function() {
            var that = $(this)[0];
            that.elements['svgelement'].value=oncoprint.getPdfInput();
            return;
        });

        $('#svg-form').submit(function() {
            var that = $(this)[0];
            that.elements['xml'].value=oncoprint.getPdfInput();
            return;
        });
    });

    return boilerplate;
};
