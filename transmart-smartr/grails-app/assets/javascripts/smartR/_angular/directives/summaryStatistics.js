//# sourceURL=summaryStatistics.js

'use strict';

window.smartRApp.directive('summaryStats', [
    '$rootScope',
    function($rootScope) {
        return {
            restrict: 'E',
            scope: {
                summaryData: '='
            },
            templateUrl: $rootScope.smartRPath +  '/js/smartR/_angular/templates/summaryStatistics.html'
        };
    }
]);
