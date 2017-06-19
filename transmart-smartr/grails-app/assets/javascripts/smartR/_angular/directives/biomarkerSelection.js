//# sourceURL=biomarkerSelection.js

'use strict';

window.smartRApp.directive('biomarkerSelection', ['$rootScope', function($rootScope) {

    return {
        restrict: 'E',
        scope: {
            biomarkers: '='
        },
        templateUrl: $rootScope.smartRPath + '/js/smartR/_angular/templates/biomarkerSelection.html',
        controller: function ($scope) {
            if (!$scope.biomarkers) {
                $scope.biomarkers = [];
            }

            var input = $('#heim-input-txt-identifier');
            input.autocomplete({
                source: function(request, response) {
                    var term = request.term;
                    if (term.length < 2) {
                        return function() {
                            return response({rows: []});
                        };
                    }
                    return getIdentifierSuggestions(
                        term,
                        function(grailsResponse) {
                            // convert Grails response to what jqueryui expects
                            // grails response looks like this:
                            // { "id": 1842083, "source": "", "keyword": "TPO", "synonyms":
                            // "(TDH2A, MSA, TPX)", "category": "GENE", "display": "Gene" }
                            var r = grailsResponse.rows.map(function(v) {
                                return {
                                    label: v.keyword,
                                    value: v
                                }
                            });
                            return response(r);
                        }
                    );
                },
                minLength: 2
            });
            input.data('ui-autocomplete')._renderItem = function(ul, item) {
                var value = item.value;
                return jQuery('<li class="ui-menu-item" role="presentation">' +
                    '<a class="ui-corner-all">' +
                    '<span class="category-gene">' + value.display + '&gt;</span>&nbsp;' +
                    '<b>' + value.keyword + '</b>&nbsp;' + value.synonyms + '</a></li>').appendTo(ul);
            };
            input.on('autocompleteselect',
                function(event, ui) {
                    var v = ui.item.value;

                    // check if the item is not in the list yet
                    if ($scope.biomarkers.filter(function(b) {
                                return b.id == v.id;
                            }).length == 0) {

                        // add the biomarker to the list
                        $scope.biomarkers.push({
                            id: v.id,
                            type: v.display,
                            name: v.keyword,
                            synonyms: v.synonyms
                        });
                        $scope.$apply();
                    }
                    this.value = '';
                    return false;
                }
            );
            input.on('autocompletefocus',
                function(event, ui) {
                    var v = ui.item.value;
                    this.value = v.display + ' ' + v.keyword;
                    return false;
                }
            );

            var getIdentifierSuggestions = (function() {
                var curXHR = null;

                return function(term, response) {
                    if (curXHR && curXHR.state() === 'pending') {
                        curXHR.abort();
                    }

                    curXHR = jQuery.get("/transmart/search/loadSearchPathways", {
                        query: term
                    });

                    curXHR.always(function() { curXHR = null; })
                    return curXHR.then(
                        function(data) {
                            data = data.substring(5, data.length - 1);  // loadSearchPathways returns String with null (JSON).
                                                                        // This strips it off
                            response(JSON.parse(data));
                        },
                        function() {
                            response({rows: []}); // response must be called even on failure
                        }
                    );
                };
            })();

            $scope.removeIdentifier = function(item) {
                var index = $scope.biomarkers.indexOf(item);
                if (index >= 0) {
                    $scope.biomarkers.splice(index, 1);
                }
            }

        }

    };
}]);
