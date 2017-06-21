//# sourceURL=linegraph.js

'use strict';

window.smartRApp.controller('LinegraphController',
    ['$scope', 'smartRUtils', 'commonWorkflowService', function($scope, smartRUtils, commonWorkflowService) {

        commonWorkflowService.initializeWorkflow('linegraph', $scope);

        $scope.fetch = {
            disabled: false,
            running: false,
            loaded: false,
            selectedBiomarkers: [],
            button: {
                disabled: false
            },
            conceptBoxes: {
                highData: {concepts: [], valid: true},
                numData: {concepts: [], valid: true},
                catData: {concepts: [], valid: true}
            }
        };

        $scope.runAnalysis = {
            disabled: true,
            running: false,
            scriptResults: {},
            params: {}
        };

        $scope.$watch(function() {
            return $scope.fetch.conceptBoxes.highData.concepts.length + ' ' + $scope.fetch.selectedBiomarkers.length;
        },
        function() {
            if ($scope.fetch.conceptBoxes.highData.concepts.length > 0 &&
                ($scope.fetch.selectedBiomarkers.length === 0 || $scope.fetch.selectedBiomarkers.length > 10)) {
                $scope.fetch.button.disabled = true;
                $scope.fetch.button.message = 'Please select between 1 and 10 biomarker for your high dimensional data';
            } else {
                $scope.fetch.button.disabled = false;
                $scope.fetch.button.message = '';
            }
        });

        $scope.$watchGroup(['fetch.running', 'runAnalysis.running'],
            function(newValues) {
                var fetchRunning = newValues[0],
                    runAnalysisRunning = newValues[1];

                // clear old results
                if (fetchRunning) {
                    $scope.runAnalysis.scriptResults = {};
                }

                // disable tabs when certain criteria are not met
                $scope.fetch.disabled = runAnalysisRunning;
                $scope.runAnalysis.disabled = fetchRunning || !$scope.fetch.loaded;
            }
        );

    }]);

