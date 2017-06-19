//# sourceURL=commonWorkflowService.js

'use strict';

window.smartRApp.factory('commonWorkflowService', ['rServeService', '$css', function(rServeService, $css) {

    var service = {};

    service.initializeWorkflow = function(workflowName, scope) {
        service.currentScope = scope;
        // load workflow specific css
        $css.bind({
            href: scope.smartRPath + '/css/' + workflowName + '.css'
        }, scope);

        rServeService.destroyAndStartSession(workflowName);
    };

    return service;

}]);
