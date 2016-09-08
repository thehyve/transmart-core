//Open and close the file list for a given trial
function toggleFileDiv(trialNumber, dataURL)	{
    var imgExpand = "#imgFileExpand_"  + trialNumber;
    var trialDetail = "#" + trialNumber + "_files";

    // If data attribute is undefined then this is the first time opening the div, load the analysis...
    if (typeof jQuery(trialDetail).attr('data') == 'undefined')	{
        var src = jQuery(imgExpand).attr('src').replace('down_arrow_small2.png', 'ajax-loader-flat.gif');
        jQuery(imgExpand).attr('src',src);
        jQuery.ajax({
            url:dataURL,
            success: function(response) {
                jQuery(imgExpand).attr('src', jQuery(imgExpand).attr('src').replace('ajax-loader-flat.gif', 'up_arrow_small2.png'));
                jQuery(trialDetail).addClass("gtb1");
                jQuery(trialDetail).html(response);
                jQuery(trialDetail).addClass("analysesopen");
                jQuery(trialDetail).attr('data', true);// Add an attribute that we will use as a flag so we don't need to load the data multiple times
            },
            error: function(xhr) {
                console.log('Error!  Status = ' + xhr.status + xhr.statusText);
            }
        });
    } else	{
        var src = jQuery(imgExpand).attr('src').replace('up_arrow_small2.png', 'down_arrow_small2.png');
        if (jQuery(trialDetail).attr('data') == "true")	{
            jQuery(trialDetail).attr('data',false);
            jQuery(trialDetail).removeClass("analysesopen");
        } else	{
            src = jQuery(imgExpand).attr('src').replace('down_arrow_small2.png', 'up_arrow_small2.png');
            jQuery(trialDetail).attr('data',true);
            jQuery(trialDetail).addClass("analysesopen");
        }
        jQuery(imgExpand).attr('src',src);
        jQuery(trialDetail).toggle();
    }
    return false;
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

jQuery(document).ready(function() {


    jQuery('body').on('click', '#results-div .foldericon.addcart', function() {
        var id = jQuery(this).attr('name');
        jQuery(this).removeClass("addcart").addClass("removecart");
        jQuery('#' + id + '-filerow').addClass("hi");
        jQuery('#cartcount').hide();

        jQuery.ajax({
            url:exportAddURL,
            data: {id: id},
            success: function(response) {
                jQuery('#cartcount').show().text(response);
            },
            error: function(xhr) {
                jQuery('#cartcount').show();
            }
        });
    });

    jQuery('body').on('click', '#results-div .foldericon.removecart', function() {
        var id = jQuery(this).attr('name');
        jQuery(this).removeClass("removecart").addClass("addcart");
        jQuery('#' + id + '-filerow').removeClass("hi");
        jQuery('#cartcount').hide();

        jQuery.ajax({
            url:exportRemoveURL,
            data: {id: id},
            success: function(response) {
                jQuery('#cartcount').show().text(response);
            },
            error: function(xhr) {
                jQuery('#cartcount').show();
            }
        });
    });

    jQuery('body').on('click', '#results-div .addall', function() {
        var nameelements = jQuery(this).closest('table').find('.foldericon.addcart');
        var ids = [];
        for (var i = 0; i < nameelements.size(); i++) {
            var id = nameelements[i];
            ids.push(jQuery(id).attr('name'));
            jQuery(id).removeClass("addcart").addClass("removecart");
            jQuery('#' + jQuery(id).attr('name') + '-filerow').addClass("hi");
        }

        jQuery('#cartcount').hide();

        jQuery.ajax({
            url:exportAddURL,
            data: {id: ids.join(",")},
            success: function(response) {
                jQuery('#cartcount').show().text(response);
            },
            error: function(xhr) {
                jQuery('#cartcount').show();
            }
        });
    });

    jQuery('body').on('click', '#results-div .foldericon.deletefile', function() {
        var id = jQuery(this).attr('name');
        var deleteFileObject = jQuery(this);
        if (confirm("Are you sure you want to delete this file?")) {
            jQuery.ajax({
                url:deleteFileURL,
                data: {id: id},
                success: function(response) {
                    //Reload the contents of the clicked table!
                    var targetElement = deleteFileObject.closest('.detailexpand');
                    var expId = targetElement.attr('name');
                    jQuery.ajax({
                        url: folderFilesURL + "?id=" + expId,
                        success: function(response) {
                            jQuery(targetElement).html(response);
                        },
                        error: function(xhr) {
                            console.log('Error!  Status = ' + xhr.status + xhr.statusText);
                        }
                    });
                },
                error: function(xhr) {
                    alert(xhr.message);
                }
            });
        }
    });

    jQuery('#cartbutton').click(function() {
        jQuery.ajax({
            url:exportViewURL,
            success: function(response) {
                jQuery('#exportOverlay').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
            }
        });
        jQuery('#exportOverlay').html('').addClass('ajaxloading');
        jQuery('#exportOverlay').fadeToggle();
    });

    jQuery('#exportOverlay').on('click', '.exporttableremove', function() {

        var row = jQuery(this).closest("tr");
        var id = row.attr('name');

        jQuery('#cartcount').hide();

        jQuery.ajax({
            url:exportRemoveURL,
            data: {id: id},
            success: function(response) {
                row.remove();
                jQuery('#cartcount').show().text(response);
                updateExportCount();
                jQuery('.search-results-table').find(".exportaddspan[name='" + id + "']").addClass("foldericon").removeClass('removecart').addClass('addcart');
                jQuery('#' + id + '-filerow').removeClass("hi");
            },
            error: function(xhr) {
                jQuery('#cartcount').show();
            }
        });
    });

    jQuery('#exportOverlay').on('click', '.greybutton.export', function() {

        var checkboxes = jQuery('#exporttable input:checked');
        var ids = [];
        for (i = 0; i < checkboxes.size(); i++) {
            ids.push(jQuery(checkboxes[i]).attr('name'));
        }

        if (ids.size() == 0) {return false;}

        window.location = exportURL + "?id=" + ids.join(',');


        for(j=0; j<ids.size(); j++){
            var id = ids[j];
            var i=0;

            jQuery('#cartcount').hide();

            jQuery.ajax({
                url:exportRemoveURL,
                data: {id: id},
                success: function(response) {
                    jQuery(checkboxes[i]).closest("tr").remove();
                    console.log(jQuery(checkboxes[i]).attr('name'));
                    jQuery('#cartcount').show().text(response);
                    updateExportCount();
                    jQuery('.search-results-table').find(".exportaddspan[name='" + ids[i] + "']").addClass("foldericon").addClass("addcart").addClass("link");
                    jQuery('#' + id + '-filerow').removeClass("hi");
                    i=i+1;
                },
                error: function(xhr) {
                    jQuery('#cartcount').show();
                }
            });
        }
    });

    jQuery('body').on('click', '#closeexport', function() {
        jQuery('#exportOverlay').fadeOut();
    });

    jQuery('body').on('click', function(e) {

        if (!jQuery(e.target).closest('#exportOverlay').length
            && !jQuery(e.target).closest('#cartbutton').length
            && jQuery(e.target).attr('id') != 'cartbutton') {

            if (jQuery('#exportOverlay').is(':visible')) {
                jQuery('#exportOverlay').fadeOut();
            }
        }
    });
});