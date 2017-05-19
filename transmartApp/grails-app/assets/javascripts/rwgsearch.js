//# sourceURL=rwgsearch.js

window.rwgModel = {
    jquery: jQuery({}),

    on: function rwgModel_on(eventName, func) {
        this.jquery.on(eventName, function() {
			/* don't call callback with event object as first argument */
            Array.prototype.shift.apply(arguments);
            func.apply(this, arguments);
        });
        return this;
    },
    onWithEvent: function rwgModel_on() {
         
        this.jquery.on.apply(this.jquery, arguments);
        return this;
    },
    trigger: function rwgModel_trigger() {
        if (arguments.length == 2 && arguments[1] instanceof Array) {
            arguments = [arguments[0], [arguments[1]]];
        }
        this.jquery.trigger.apply(this.jquery, arguments);
        return this;
    },

    /**
     * {
     *   operator: 'AND' | 'OR',
     *   fieldTerms: {
     *     <field name>: {
     *       operator: 'AND' | 'OR'
     *       searchTerms: [
     *         {
     *           literalTerm: <>,
     *           luceneTerm: <>,
     *         }
     *       ]
     *     }
     *   }
     */
    searchSpecification: {
        operator: 'AND',
        fieldTerms: {},
    },
    addSearchTerm: function rwgModel_addSearchTerm(fieldName, value, literal) {
        literal = literal || false;
        var fieldTerm = this.searchSpecification.fieldTerms[fieldName];
        var searchTerm;

        if (!fieldTerm) {
            fieldTerm = this.searchSpecification.fieldTerms[fieldName] = {
                operator: 'OR',
                searchTerms: [],
            };
        }
        if (literal) {
            searchTerm = { literalTerm: value };
        } else {
            searchTerm = { luceneTerm: value };
        }

        // don't do nothing if the term is already there
        var prev = fieldTerm.searchTerms.find(function(el) {
            return el.literalTerm == searchTerm.value &&
                el.luceneTerm == searchTerm.luceneTerm;
        });
        if (prev) {
            return;
        }

        fieldTerm.searchTerms.push(searchTerm);

        this.trigger('search_specification', this.searchSpecification);
    },
    removeSearchTerm: function rwgModel_removeSearchTerm(fieldName, value, literal) {
        var searchTerm;
        var fieldSpec = this.searchSpecification.fieldTerms[fieldName];
        if (!fieldSpec) {
            console.error('Cannot remove term for field, no such field with terms: ' + fieldName);
            return;
        }

        if (literal) {
            searchTerm = { literalTerm: value };
        } else {
            searchTerm = { luceneTerm: value };
        }


        fieldSpec.searchTerms = fieldSpec.searchTerms.filter(function(el) {
            return searchTerm.literalTerm != el.literalTerm ||
                searchTerm.luceneTerm != el.luceneTerm;
        });
        if (fieldSpec.searchTerms.length == 0) {
            delete this.searchSpecification.fieldTerms[fieldName];
        }

        this.trigger('search_specification', this.searchSpecification);
    },
    alterSearchSpecification: function rwgModel_alterSearchSpecification(func) {
        // generic function for changes in searchSpecification,
        // with notification at the end
        func(this.searchSpecification);
        this.trigger('search_specification', this.searchSpecification);
    },
    clearSearchTerms: function rwgSearch_clearSearchTerms() {
        this.searchSpecification.fieldTerms = {};
        this.trigger('search_specification', this.searchSpecification);
    },
    numberOfResults: undefined,
    _returnedFolders: undefined, // ids only
    get returnedFolders() { return this._returnedFolders; },
    set returnedFolders(v) {
        this._returnedFolders = v;
        this.trigger('folders_list', v);
    },
    _returnedConcepts: undefined, // concept paths
    get returnedConcepts() { return this._returnedConcepts; },
    set returnedConcepts(v) {
        this._returnedConcepts = v;
        this.trigger('concepts_list', v);
    },
    _searchError: undefined,
    get searchError() { return this._searchError; },
    set searchError(v) {
        this._searchError = v;
        this.trigger('search_error', v);
    },

    _searchCategories: undefined, /* as gotten from service, map from field name to display name */
    get searchCategories() { return this._searchCategories; },
    set searchCategories(v) {
        this._searchCategories = v;
        this.trigger('search_categories', v);
    },
    _currentCategory: undefined, /* field name */
    get currentCategory() { return this._currentCategory; },
    set currentCategory(v) {
        this._currentCategory = v;
        this.trigger('current_category', v);
    },

    requiredField: null,
    _startingFilterResults: {}, /* map required field -> data returned by getTopTerms */
    get startingFilterResults() { return this._startingFilterResults[this.requiredField]; },
    set startingFilterResults(v) {
        this._startingFilterResults[this.requiredField] = v;
        if (!this.currentFilterResults && v !== undefined) {
            this.trigger('current_filters', v);
        }
    },
    _currentFilterResults: undefined,  /* same format; null to use startingFilterResults  */
    get currentFilterResults() { return this._currentFilterResults; },
    set currentFilterResults(v) {
        var prevValue = this._currentFilterResults;
        this._currentFilterResults = v;
        if (v) { /* new current value */
            this.trigger('current_filters', v);
        } else if (prevValue) {
            this.trigger('current_filters', this.startingFilterResults);
        }
    },

    serialize: function rwgModel_serialize() {
        var keys = ['searchCategories',
            'currentCategory',
            '_startingFilterResults',
            'searchSpecification'];
        var res = {};
        keys.forEach(function(k) { res[k] = this[k]; }.bind(this));
        return JSON.stringify(res);
    },
    unserialize: function rwgModel_unserialize(str) {
        try {
            var obj = JSON.parse(str);
        } catch (e) {
            console.error('Error reading rwgModel saved data', e);
            return;
        }
        Object.keys(obj).forEach(function(k) { this[k] = obj[k]; }.bind(this));
        this.startingFilterResults = this.startingFilterResults; /* maybe trigger event */
        this.trigger('search_specification', this.searchSpecification);
    },
};

window.rwgView = {
    searchCategoriesEl: /* #search-categories */   undefined,
    searchInputEl:      /* #search-ac */           undefined,
    filterBrowserEl:    /* #filter-browser */      undefined,
    boxSearchEl:        /* #box-search */          undefined,
    activeSearchEl:     /* #active-search-div */   undefined, // child of box-search
    globalOperatorEl:   /* #globaloperator */      undefined,
    clearFiltersEl:     /* #clearbutton */         undefined,
    nodeSearchMsgEl:    /* #node-search-message */ undefined,

    config: {
        requiredField: undefined,
        onConceptsListChanges: function() {},
        onFoldersListChanges: function() {},
    },

    init: function rwgView_init(config) {
        this.config = jQuery.extend(this.config, config);
         
        window.rwgModel.requiredField = this.config.requiredField;

        // find elements
        this.searchCategoriesEl = jQuery('#search-categories');
        this.searchInputEl      = jQuery('#search-ac');
        this.filterBrowserEl    = jQuery('#filter-browser');
        this.boxSearchEl        = jQuery('#box-search');
        this.activeSearchEl     = jQuery('#active-search-div');
        this.globalOperatorEl   = jQuery('#globaloperator');
        this.clearFiltersEl     = jQuery('#clearbutton');
        this.nodeSearchMsgEl    = jQuery('#node-search-message');

        this.bindToModel();
        this.bindUIEvents();
        var loadedFromStorage = false;
        if (sessionStorage.getItem('rwgModel')) {
            storedRwgModel = sessionStorage.getItem('rwgModel');
            rwgModel.unserialize(storedRwgModel);
            loadedFromStorage = true;
        }
        jQuery(window).unload(function() {
            sessionStorage.setItem('rwgModel', rwgModel.serialize());
        });

        if (rwgModel.searchCategories === undefined) {
            rwgController.fetchCategories();
        }
        if (rwgModel.startingFilterResults === undefined) {
            rwgController.fetchStartingFilterResults(rwgModel.requiredField);
        }

        if (!loadedFromStorage) {
            rwgModel.clearSearchTerms(); // to trigger loading of root elements
        }
    },

    bindToModel: function rwgView_bindToModel() {
        rwgModel.on('current_category', this.currentCategoryChanges.bind(this));
        rwgModel.on('search_categories', this.searchCategoriesChange.bind(this));
        rwgModel.on('current_filters', this.currentFilterResultsChange.bind(this));
        rwgModel.on('search_specification', this.searchSpecificationChanges.bind(this));
        rwgModel.on('search_specification', this.selectedFiltersChange.bind(this));
        rwgModel.on('search_specification', function (data) {
            rwgController.performSearch(data, rwgModel.requiredField);
        });
        rwgModel.on('concepts_list', function (data) {
            this.config.onConceptsListChanges.call(this, data, rwgModel.numberOfResults);
        }.bind(this));
        rwgModel.on('folders_list', this.config.onFoldersListChanges.bind(this));
        rwgModel.on('search_error', this.searchError.bind(this));
    },
    bindUIEvents: function rwgView_bindUIEvents() {
        this.activeSearchBindEvents();
        this.filterItemsBindEvents();
    },

    searchCategoriesChange: function rwgView_searchCategoriesChange(searchCategories) {
        var addCategory = function addCategory(field, text) {
            var el = jQuery('<option></option>')
                .attr('value', field)
                .text(text);

            this.searchCategoriesEl.append(el)
        }.bind(this);

        for (var category in searchCategories) {
            addCategory(category, searchCategories[category]);
        }

        // bind events
        this.searchCategoriesEl.unbind('change');
        this.searchCategoriesEl.change(function() {
            rwgController.changeCurrentCategory(this.searchCategoriesEl.val());
        }.bind(this));

        (function rwgView_setupAutocomplete() {
            this.searchInputEl.autocomplete({
                position: { my: 'left top', at: 'left bottom', collision: 'none' },
                // source set in currentCategoryChanges
                minLength: 1,
                select: function (event, ui) {
                    rwgController.addSearchTerm(ui.item.category, ui.item.value, true);
                    this.searchInputEl.val('');
                    return false;
                }.bind(this),
            }).data("uiAutocomplete")._renderItem = function(ul, item) {
                var a = jQuery('<a>');
                var span = jQuery('<span>')
                    .text(searchCategories[item.category] + '>');
                var b = jQuery('<b>').text(item.value);
                a.append(span)
                    .append(document.createTextNode(' '))
                    .append(b)
                    .append(document.createTextNode(' (' + item.count + ')'));

                return jQuery('<li>')
                    .data("item.autocomplete", item)
                    .append(a)
                    .appendTo(ul);
            };

            // Capture the enter key on the slider and fire off the search event on the autocomplete
            this.searchCategoriesEl.unbind('keypress');
            this.searchCategoriesEl.keypress(function(event) {
                if (event.which == 13) {
                    this.searchInputEl.autocomplete('search');
                }
            });

            this.searchInputEl.unbind('keypress');
            this.searchInputEl.keypress(function(event) {
                if (event.which != 13 || this.searchInputEl.val() == '') {
                    return true;
                }

                rwgController.addSearchTerm(rwgModel.currentCategory, this.searchInputEl.val(), false);
                this.searchInputEl.val('');
                return false;
            }.bind(this));
        }.bind(this))();
    },
    currentCategoryChanges: function rwgView_currentCategoryChanges(currentCategory) {
        this.searchCategoriesEl.val(currentCategory);
        this.searchInputEl.autocomplete('option',
            'source', window.searchURLs.autoComplete
            + '?category=' + encodeURIComponent(currentCategory)
            + '&requiredField=' + encodeURIComponent(rwgModel.requiredField));
    },
    currentFilterResultsChange: function rwgView_currentFilterResultsChange(currentResults) {
        function addField(fieldData) {
            var category = fieldData.category;
            var choices = fieldData.choices;
            var titleDiv = jQuery('<div>')
                .addClass('filtertitle')
                .attr('name', category.field)
                .text(category.displayName);

            var contentDiv = jQuery('<div>')
                .addClass('filtercontent')
                .data('fieldName', category.field)
                .hide();

            choices.forEach(function(ch) {
                var newItem = jQuery('<div></div>')
                    .addClass('filteritem')
                    .data('fieldName', category.field)
                    .data('value', ch.value)
                    .text(ch.value + ' (' + ch.count + ')');
                contentDiv.append(newItem);
            });

            this.filterBrowserEl.append(titleDiv);
            this.filterBrowserEl.append(contentDiv);
        }

        this.filterBrowserEl.find('.filtertitle, .filtercontent').remove();
        currentResults.forEach(addField.bind(this));
        this.selectedFiltersChange(rwgModel.searchSpecification);
        this.filterBrowserEl.removeClass('ajaxloading');
    },
    selectedFiltersChange: function rwgView_selectedFiltersChange(searchSpecification) {
        // clear current selected classes
        this.filterBrowserEl.find('.selected').each(function() {
            jQuery(this).removeClass('selected');
        });
        var filterTitles = this.filterBrowserEl.find('.filtertitle');

        Object.keys(searchSpecification.fieldTerms).forEach(function(fieldName) {
            var searchTerms = searchSpecification.fieldTerms[fieldName].searchTerms || [];
            var titleElement = filterTitles.filter(function() {
                return jQuery(this).attr('name') == fieldName;
            });
            if (titleElement.size() == 0) {
                return;
            }

            searchTerms.forEach(function(searchTerm) {
                titleElement
                    .next('.filtercontent')
                    .children('.filteritem')
                    .filter(function() {
                        var jq = jQuery(this);
                        return jq.data('value') == searchTerm.literalTerm;
                    })
                    .addClass('selected')
                    .parent() // also make sure the filter content is being show
                    .show();
            });
        });
    },
    filterItemsBindEvents: function rwgView_filterItemsBindEvents() {
        this.filterBrowserEl.on('click', '.filteritem', function () {
            var jq = jQuery(this);
            var selecting = !jq.hasClass('selected');
            if (selecting) {
                rwgController.addSearchTerm(jq.data('fieldName'), jq.data('value'), true);
            } else {
                rwgController.removeSearchTerm(jq.data('fieldName'), jq.data('value'), true);
            }
        });
        this.filterBrowserEl.on('click', '.filtertitle', function () {
            jQuery(this).next('.filtercontent').toggle('fast');
        });

    },

    searchSpecificationChanges: function rwgView_searchSpecificationChanges(searchSpecification) {
        this.activeSearchEl.removeClass('search-error');

        if (searchSpecification.operator == 'AND') {
            this.globalOperatorEl.attr('class', 'andor and');
        } else {
            this.globalOperatorEl.attr('class', 'andor or');
        }

        var elements = [];
        function createSpacer(operator) {
            return jQuery('<span>')
                .addClass('spacer')
                .text(operator == 'AND' ? 'and ' : 'or ')
        }
        function addCategory(fieldName, specs) {
            if (elements.length > 0) { // not the first category
                var separator = jQuery('<span>')
                    .addClass('category_join')
                    .append(document.createTextNode(searchSpecification.operator))
                    .append(jQuery('<span class="h_line"></span>'))
                elements.push(separator);
            }

            var title = jQuery('<span>')
                .addClass('category_label')
                .text(rwgModel.searchCategories[fieldName] + '\u00A0>');
            elements.push(title, document.createTextNode('\u00A0'));

            specs.searchTerms.forEach(function(term, i) {
                if (i > 0) {
                    elements.push(createSpacer(specs.operator));
                }

                var value = term.literalTerm ? term.literalTerm : term.luceneTerm;
                var term = jQuery('<span>')
                    .addClass('term')
                    .addClass(term.literalTerm ? 'literal-term' : 'lucene-term')
                    .text((term.literalTerm ? term.literalTerm : term.luceneTerm) + '\u00A0')
                    .append(
                        jQuery('<a href="#">')
                            .addClass('term-remove')
                            .data('termValue', value)
                            .data('termLiteral', term.literalTerm ? true : false)
                            .data('fieldName', fieldName)
                            .append(jQuery('<img alt="remove">')
                                .attr('src', window.searchURLs.crossImage)
                                .data('fieldName', fieldName)))
                    .append(document.createTextNode('\u00A0'))
                elements.push(term);
            });
            if (specs.searchTerms.length > 1) {
                elements.push(
                    jQuery('<div>')
                        .addClass('andor')
                        .addClass(specs.operator == 'AND' ? 'and' : 'or')
                        .data('fieldName', fieldName)
                        .text('\u00A0'));
            }
        }

        Object.keys(searchSpecification.fieldTerms).forEach(function(fieldName) {
            addCategory.call(this, fieldName, searchSpecification.fieldTerms[fieldName]);
        }.bind(this));

        this.activeSearchEl.empty();
        this.activeSearchEl.append(elements);
    },

    activeSearchBindEvents: function rwgView_activeSearchBindEvents() {
        this.boxSearchEl.on('click', '.andor', function() {
            var jq = jQuery(this);
            if (jq.attr('id') == 'globaloperator') {
                rwgController.switchGlobalOperator();
            } else {
                rwgController.switchFieldOperator(jq.data('fieldName'));
            }
        });

        this.boxSearchEl.on('click', '.term-remove', function() {
            var jq = jQuery(this);
            rwgController.removeSearchTerm(jq.data('fieldName'), jq.data('termValue'), jq.data('termLiteral'));
        });

        this.clearFiltersEl.click(rwgController.clearSearchTerms.bind(rwgController));
    },

    searchError: function rwgView_searchError() {
        this.activeSearchEl.addClass('search-error');
    },
};

window.rwgController = {
    flyingSearch: undefined,

    fetchCategories: function rwgController_fetchCategories() {
        jQuery.ajax({
            url: window.searchURLs.getSearchCategories,
            dataType: 'json',
        }).then(function(json) {
            var data = { '*': 'All' };
            jQuery.extend(data, json);
            rwgModel.searchCategories = data;
            rwgModel.currentCategory = '*'; /* reset field after fetching categories */
        }).fail(function(jqhr) { console.error('Failed getting search categories', jqhr); });
    },
    fetchStartingFilterResults: function rwgController_fetchStartingFilterResults(requiredField) {
        jQuery.ajax({
            url: window.searchURLs.getFilterCategories,
            data: { requiredField: requiredField },
            dataType: 'json',
        }).then(function(json) {
            rwgModel.startingFilterResults = json;
        }).fail(function(jqhr) { console.error('Failed getting filter categories', jqhr); })
    },
    changeCurrentCategory: function rwgController_changeCurrentCategory(fieldName) {
        rwgModel.currentCategory = fieldName;
    },
    addSearchTerm: function rwgController_addSearchterm(fieldName, value, literal) {
        rwgModel.addSearchTerm(fieldName, value, literal);
    },
    removeSearchTerm: function rwgController_removeSearchTerm(fieldName, value, literal) {
        rwgModel.removeSearchTerm(fieldName, value, literal);
    },
    clearSearchTerms: function rwgController_clearSearchTerms() {
        rwgModel.clearSearchTerms();
    },
    switchGlobalOperator: function rwgController_switchGlobalOperator() {
        rwgModel.alterSearchSpecification(function(spec) {
            spec.operator = spec.operator === 'AND' ? 'OR' : 'AND';
        });
    },
    switchFieldOperator: function rwgController_switchFieldOperator(fieldName) {
        rwgModel.alterSearchSpecification(function(spec) {
            var fieldSpec = spec.fieldTerms[fieldName];
            if (fieldSpec === undefined) {
                console.error('Try to swap field operator for field with no search terms: ' + fieldName);
                return;
            }
            fieldSpec.operator = fieldSpec.operator === 'AND' ? 'OR' : 'AND';
        });
    },
    performSearch: function rwgController_performSearch(searchSpecification, requiredField) {
        if (this.flyingSearch) {
            this.flyingSearch.abort();
        }
        var data = jQuery.extend({ requiredField: requiredField }, searchSpecification);

        if (Object.keys(searchSpecification.fieldTerms).length == 0) {
            // nothing to search
            rwgModel.returnedFolders = undefined;
            rwgModel.returnedConcepts = undefined;
            rwgModel.currentFilterResults = undefined;

            // TODO: datasetExplorer specific
            if (window.GLOBAL !== undefined) {
                window.GLOBAL.PathToExpand = '';
            }
            return;
        }

        this.flyingSearch = jQuery.ajax({
            type: 'POST',
            url: window.searchURLs.getFacetResults,
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify(data)
        })
            .then(function getFacetResults_success(json) {
            rwgModel.returnedFolders = json['folderIds'];
            rwgModel.numberOfResults = json['numFound'];
            rwgModel.returnedConcepts = json['conceptKeys'];
            rwgModel.currentFilterResults = json['facets'];
        }).fail(function(jqhr) {
            console.error('Failed getting search categories', jqhr);
            rwgModel.searchError = jqhr.responseJSON !== undefined ?
                jqhr.responseJSON.message : jqhr.responseText
        }).always(function() { this.flyingSearch = undefined; }.bind(this));
    }
};

//Method to load the search results in the search results panel and facet counts into tree
//This occurs whenever a user add/removes a search term
function showSearchResults(openInAnalyze, datasetExplorerPath)	{

    // clear stored probe Ids for each analysis
    analysisProbeIds = new Array();

    // clear stored analysis results
    jQuery('body').removeData();

    jQuery('#results-div').empty();

    // call method which retrieves facet counts and search results
    showFacetResults(openInAnalyze, datasetExplorerPath);

    //all analyses will be closed when doing a new search, so clear this array
    openAnalyses = [];

}

jQuery(document).ready(function() {

    jQuery('#sidebartoggle').click(function() {
        toggleSidebar();
    });


    jQuery('body').on('mouseenter', '.folderheader', function() {
        jQuery(this).find('.foldericonwrapper').fadeIn(150);
    });

    jQuery('body').on('mouseleave', '.folderheader', function() {
        jQuery(this).find('.foldericonwrapper').fadeOut(150);
    });

    jQuery('body').on('click', '.foldericon.add', function() {
        var id = jQuery(this).attr('name');
        jQuery(this).removeClass("foldericon").removeClass("add").removeClass("link").text("Added to cart");
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

    jQuery('body').on('click', '.foldericon.addall', function() {
        var nameelements = jQuery(this).closest('table').find('.foldericon.add');
        var ids = [];
        for (i = 0; i < nameelements.size(); i++) {
            ids.push(jQuery(nameelements[i]).attr('name'));
            jQuery(nameelements[i]).removeClass("foldericon").removeClass("add").removeClass("link").text("Added to cart");
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

    jQuery('body').on('click', '.foldericon.delete', function() {
        var id = jQuery(this).attr('name');

        if (confirm("Are you sure you want to delete this file?")) {
            jQuery.ajax({
                url:deleteFileURL,
                data: {id: id},
                success: function(response) {
                    jQuery('#files-table').html(response);
                    //Get document count and reduce by 1
                    var folderId = jQuery('#file-list-table').attr('name');
                    var documentCount = jQuery('#folder-header-' + folderId + ' .document-count');
                    if (documentCount.size() > 0) {
                        var currentValue = documentCount.text();
                        documentCount.text(currentValue - 1);
                    }
                },
                error: function(xhr) {
                    alert(xhr.message);
                }
            });
        }
    });

    jQuery('body').on('click', '.foldericon.view', function() {
        var id = jQuery(this).closest(".folderheader").attr('name');
        showDetailDialog(id);
    });

    jQuery('#metadata-viewer').on('click', '.editmetadata', function() {

        var id = jQuery(this).attr('name');

        jQuery('#editMetadataOverlay').fadeIn();
        jQuery('#editMetadata').empty().addClass('ajaxloading');

        jQuery.ajax({
            url:editMetaDataURL,
            data: {folderId: id},
            success: function(response) {
                jQuery('#editMetadata').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr.responseText);
                jQuery('#editMetadata').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#metadata-viewer').on('click', '.addassay', function() {

        var id = jQuery(this).attr('name');

        jQuery('#createAssayOverlay').fadeIn();
        jQuery('#createAssay').empty().addClass('ajaxloading');
        jQuery('#editMetadata').empty();

        jQuery.ajax({
            url:createAssayURL,
            data: {folderId: id},
            success: function(response) {
                jQuery('#createAssay').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr);
                jQuery('#createAssay').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#metadata-viewer').on('click', '.addanalysis', function() {

        var id = jQuery(this).attr('name');

        jQuery('#createAnalysisOverlay').fadeIn();
        jQuery('#createAnalysis').empty().addClass('ajaxloading');
        jQuery('#editMetadata').empty();

        jQuery.ajax({
            url:createAnalysisURL,
            data: {folderId: id},
            success: function(response) {
                jQuery('#createAnalysis').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr);
                jQuery('#createAnalysis').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#metadata-viewer').on('click', '.addfolder', function() {

        var id = jQuery(this).attr('name');

        jQuery('#createFolderOverlay').fadeIn();
        jQuery('#createFolder').empty().addClass('ajaxloading');
        jQuery('#editMetadata').empty();

        jQuery.ajax({
            url:createFolderURL + "?",
            data: {folderId: id},
            success: function(response) {
                jQuery('#createFolder').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr);
                jQuery('#createFolder').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#metadata-viewer').on('click', '.deletefolder', function() {

        var id = jQuery(this).attr('name');
        var parent = jQuery(this).data('parent');

        if (confirm("Are you sure you want to delete this folder and the files and folders beneath it?")) {
            jQuery.ajax({
                url:deleteFolderURL,
                data: {id: id},
                success: function(response) {
                    updateFolder(parent);
                    showDetailDialog(parent);
                    jQuery('.result-folder-name').removeClass('selected');
                    jQuery('#result-folder-name-' + parent).addClass('selected');
                },
                error: function(xhr) {
                    alert(xhr.message);
                }
            });
        }
    });

    jQuery('#metadata-viewer').on('click', '.addstudy', function() {

        var id = jQuery(this).attr('name');

        jQuery('#createStudyOverlay').fadeIn();
        jQuery('#createStudy').empty().addClass('ajaxloading');
        jQuery('#editMetadata').empty();

        jQuery.ajax({
            url:createStudyURL,
            data: {folderId: id},
            success: function(response) {
                jQuery('#createStudy').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr);
                jQuery('#createStudy').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#welcome-viewer').on('click', '.addprogram', function() {

        var id = jQuery(this).attr('name');

        jQuery('#createProgramOverlay').fadeIn();
        jQuery('#createProgram').empty().addClass('ajaxloading');
        jQuery('#editMetadata').empty();

        jQuery.ajax({
            url:createProgramURL,
            data: {folderId: id},
            success: function(response) {
                jQuery('#createProgram').html(response).removeClass('ajaxloading');
            },
            error: function(xhr) {
                alert(xhr);
                jQuery('#createProgram').html(response).removeClass('ajaxloading');
            }
        });
    });

    jQuery('#exportOverlay').on('click', '.greybutton.remove', function() {

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
                jQuery('#metadata-viewer').find(".exportaddspan[name='" + id + "']").addClass("foldericon").addClass("add").addClass("link").text('Add to export');
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

        jQuery('#cartcount').hide();

        jQuery.ajax({
            url:exportRemoveURL,
            data: {id: ids.join(',')},
            success: function(response) {
                for(j=0; j<ids.size(); j++){
                    jQuery(checkboxes[j]).closest("tr").remove();
                    jQuery('#cartcount').show().text(response);
                    updateExportCount();
                    jQuery('#metadata-viewer').find(".exportaddspan[name='" + ids[j] + "']").addClass("foldericon").addClass("add").addClass("link").text('Add to export');
                }
            },
            error: function(xhr) {
                jQuery('#cartcount').show();
            }
        });
    });

    jQuery('body').on('click', '#closeexport', function() {
        jQuery('#exportOverlay').fadeOut();
    });

    jQuery('body').on('click', '#closefilter', function() {
        jQuery('#filter-browser').fadeOut();
    });

    jQuery('body').on('click', '#closeedit', function() {
        jQuery('#editMetadataOverlay').fadeOut();
    });

    jQuery('body').on('click', '#closeassay', function() {
        jQuery('#createAssayOverlay').fadeOut();
    });

    jQuery('body').on('click', '#closeanalysis', function() {
        jQuery('#createAnalysisOverlay').fadeOut();
    });

    jQuery('body').on('click', '#closefolder', function() {
        jQuery('#createFolderOverlay').fadeOut();
    });

    jQuery('body').on('click', '#closestudy', function() {
        jQuery('#createStudyOverlay').fadeOut();
    });
    jQuery('body').on('click', '#closeprogram', function() {
        jQuery('#createProgramOverlay').fadeOut();
    });

    //Close export and filter overlays on click outside
    jQuery('body').on('click', function(e) {

        if (!jQuery(e.target).closest('#exportOverlay').length
            && !jQuery(e.target).closest('#cartbutton').length
            && jQuery(e.target).attr('id') != 'cartbutton') {

            if (jQuery('#exportOverlay').is(':visible')) {
                jQuery('#exportOverlay').fadeOut();
            }
        }

        if (!jQuery(e.target).closest('#filter-browser').length
            && !jQuery(e.target).closest('#filterbutton').length
            && jQuery(e.target).attr('id') != 'filter-browser') {

            if (jQuery('#filter-browser').is(':visible')) {
                jQuery('#filter-browser').fadeOut();
            }
        }
    });

    jQuery('#results-div').on('click', '.result-folder-name', function() {
        jQuery('.result-folder-name').removeClass('selected');
        jQuery(this).addClass('selected');
    });

    jQuery('#logocutout').on('click', function() {
        jQuery('#metadata-viewer').empty();

        jQuery('#welcome-viewer').empty().addClass('ajaxloading');
        jQuery('#welcome-viewer').load(welcomeURL, {}, function() {
            jQuery('#welcome-viewer').removeClass('ajaxloading');
        });
    });

    jQuery('#cartbutton').click(function() {
        jQuery.ajax({
            url:exportViewURL,
            success: function(response) {
                jQuery('#exportOverlay').html(response);
            },
            error: function(xhr) {
            }
        });
        jQuery('#exportOverlay').fadeToggle();
    });

    jQuery('#filterbutton').click(function() {
        jQuery('#filter-browser').fadeToggle();
    });

    jQuery(document).ready(function() {
        window.rwgView.init.call(window.rwgView, window.rwgSearchConfig);
    });
});


function updateFolder(id) {

    var imgExpand = "#imgExpand_"  + id;
    var src = jQuery(imgExpand).attr('src').replace('folderplus.png', 'ajax-loader-flat.gif').replace('folderminus.png', 'ajax-loader-flat.gif');
    jQuery(imgExpand).attr('src',src);

    jQuery.ajax({
        url:folderContentsURL,
        data: {id: id, auto: false},
        success: function(response) {
            jQuery('#' + id + '_detail').html(response).addClass('gtb1').addClass('analysesopen').attr('data', true);

            //check if the object has children
            if(jQuery('#' + id + '_detail .search-results-table .folderheader').size() > 0){
                jQuery(imgExpand).attr('src', jQuery(imgExpand).attr('src').replace('ajax-loader-flat.gif', 'folderminus.png'));
            }else{
                jQuery(imgExpand).attr('src', jQuery(imgExpand).attr('src').replace('ajax-loader-flat.gif', 'folderleaf.png'));
            }
        },
        error: function(xhr) {
            console.log('Error!  Status = ' + xhr.status + xhr.statusText);
        }
    });
}

//display search results numbers
function displayResultsNumber(){
    if(resultNumber!=""){
        var jsonNumbers = JSON.parse(resultNumber);

        jQuery('#welcome-viewer').empty();
        jQuery('#metadata-viewer').empty();
        var htmlResults="<div style='margin: 10px;padding: 10px;'><h3 class='rdc-h3'>Search results by type</h3>";
        htmlResults+="<table class='details-table'>";
        htmlResults+="<thead><tr><th class='columnheader'>Object</th><th class='columnheader'>Number of results</th></tr></thead>";
        htmlResults+="<tr class='details-row odd'><td class='columnname'>Programs</td><td class='columnvalue'>"+jsonNumbers.PROGRAM+"</td></tr>";
        htmlResults+="<tr class='details-row odd'><td class='columnname'>Studies</td><td class='columnvalue'>"+jsonNumbers.STUDY+"</td></tr>";
        htmlResults+="<tr class='details-row odd'><td class='columnname'>Assays</td><td class='columnvalue'>"+jsonNumbers.ASSAY+"</td></tr>";
        htmlResults+="<tr class='details-row odd'><td class='columnname'>Analyses</td><td class='columnvalue'>"+jsonNumbers.ANALYSIS+"</td></tr>";
        htmlResults+="<tr class='details-row odd'><td class='columnname'>Folders</td><td class='columnvalue'>"+jsonNumbers.FOLDER+"</td></tr>";
        htmlResults+="</table></div>";
        jQuery('#metadata-viewer').html(htmlResults);
    }
}

//Globally prevent AJAX from being cached (mostly by IE)
jQuery.ajaxSetup({
    cache: false
});

