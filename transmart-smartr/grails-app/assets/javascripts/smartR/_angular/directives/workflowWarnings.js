//# sourceURL=workflowWarnings.js

'use strict';

window.smartRApp.directive('workflowWarnings', [
    '$rootScope',
    function($rootScope) {
        return {
            restrict: 'E',
            scope: {
                warnings: '='
            },
            templateUrl: $rootScope.smartRPath +  '/js/smartR/_angular/templates/workflowWarnings.html',
            link: function(scope) {
                scope.$watch('warnings', function() {
                    scope.visibility = $.isEmptyObject(scope.warnings) ? 'hidden' : 'visible';
                    scope.text = '';
                    for (var warn in scope.warnings) {
                        if (scope.warnings.hasOwnProperty(warn)) {
                            scope.text += scope.warnings[warn] + '\n';
                        }
                    }
                }, true);
            }
        };
    }
]);
