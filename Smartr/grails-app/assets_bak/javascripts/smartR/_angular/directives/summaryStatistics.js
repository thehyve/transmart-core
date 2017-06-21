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
            templateUrl: $rootScope.smartRPath +  '/js/smartr/_angular/templates/summaryStatistics.html'
        };
    }
]);
