//# sourceURL=workflowTab.js

'use strict';

window.smartRApp.directive('workflowTab', ['smartRUtils', function(smartRUtils) {
    return {
        restrict: 'E',
        scope: {
            name: '@tabName',
            disabled: '='
        },
        require: '^tabContainer',
        transclude: true,
        template: '<ng-transclude-replace></ng-transclude-replace>',
        link: function(scope, element, attrs, tabContainerCtrl) {
            var id = 'fragment-' + smartRUtils.makeSafeForCSS(scope.name);
            scope.id = id;
            element[0].id = id;
            tabContainerCtrl.addTab(scope);
        }
    };
}]);
