//# sourceURL=sortingCriteria.js

'use strict';

window.smartRApp.directive('sortingCriteria', [
    '$rootScope',
    function($rootScope) {
        return {
            restrict: 'E',
            scope: {
                criteria : '=',
                samples: '=',
                subsets: '='
            },
            templateUrl: $rootScope.smartRPath +  '/js/smartr/_angular/templates/sortingCriteria.html'
        };
    }
]);
