//# sourceURL=cohortSummaryInfo.js

'use strict';

window.smartRApp.directive('cohortSummaryInfo', [function() {

    return {
        restrict: 'E',
        template: '<span id="sr-cohort-selection" style="font-size: 11px;"></span>',
        controller: function($scope, $element) {
            var span = $element.children()[0];

            function _showCohortInfo() {
                var cohortsSummary = '';

                for(var i = 1; i <= GLOBAL.NumOfSubsets; i++) {
                    var currentQuery = getQuerySummary(i);
                    if(currentQuery !== '') {
                        cohortsSummary += '<br/>Subset ' + (i) + ': <br/>';
                        cohortsSummary += currentQuery;
                        cohortsSummary += '<br/>';
                    }
                }
                if (!cohortsSummary) {
                    cohortsSummary = '<br/>WARNING: No subsets have been selected! Please go to the "Comparison" tab and select your subsets.';
                }

                span.innerHTML = cohortsSummary;
            }

            // Trigger for update is clicking the SmartR panel item. Maybe there is a more elegant way?
            $scope.$evalAsync(function() {
                _showCohortInfo(); // set it one time initially
                $('#resultsTabPanel__smartRPanel').on('click', function() {
                    _showCohortInfo();
                });
            });
        }
    };

}]);
