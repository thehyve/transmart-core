//# sourceURL=volcanoplot.js

'use strict';

window.smartRApp.controller('VolcanoplotController', [
    '$scope',
    'smartRUtils',
    'commonWorkflowService',
    function($scope, smartRUtils, commonWorkflowService) {

        commonWorkflowService.initializeWorkflow('volcanoplot', $scope);

        $scope.fetch = {
            disabled: false,
            running: false,
            loaded: false,
            conceptBoxes: {
                highDimensional: {concepts: [], valid: false}
            }
        };

        $scope.runAnalysis = {
            params: {},
            disabled: true,
            running: false,
            scriptResults: {}
        };

        $scope.$watchGroup(['fetch.running', 'runAnalysis.running'], function(newValues) {
            var fetchRunning = newValues[0],
                runAnalysisRunning = newValues[1];

            // clear old results
            if (fetchRunning) {
                $scope.runAnalysis.scriptResults = {};
            }

            // disable tabs when certain criteria are not met
            $scope.fetch.disabled = runAnalysisRunning;
            $scope.runAnalysis.disabled = fetchRunning || !$scope.fetch.loaded;
        });

    }]);

