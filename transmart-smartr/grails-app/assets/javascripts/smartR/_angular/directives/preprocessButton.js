//# sourceURL=preprocessButton.js

'use strict';

window.smartRApp.directive('preprocessButton', [
    'rServeService',
    '$rootScope',
    function(rServeService, $rootScope) {
        return {
            restrict: 'E',
            scope: {
                running: '=?',
                params: '=?',
                showSummaryStats: '=',
                summaryData: '=',
                allSamples: '=?',
                numberOfRows: '=?',
                projection: '@?'
            },
            templateUrl: $rootScope.smartRPath + '/js/smartR/_angular/templates/preprocessButton.html',
            link: function(scope, element) {

                var template_btn = element.children()[0];
                var template_msg = element.children()[1];

                var _onSuccess = function() {
                    template_msg.innerHTML = 'Task complete! Go to the "Run Analysis" tab to continue.';
                    template_btn.disabled = false;
                    scope.running = false;
                };

                var _onFail = function(msg) {
                    template_msg.innerHTML = 'Error: ' + msg;
                    template_btn.disabled = false;
                    scope.running = false;
                };

                // we add this conditional $watch because there is some crazy promise resolving for allSamples
                // going on. This is a workaround which observes allSamples and uses it as criteria for successful
                // completion. FIXME
                scope.$watch('summaryData', function(newValue) {
                    if (scope.summaryData &&
                            scope.showSummaryStats &&
                            scope.running &&
                            Object.keys(newValue).indexOf('subsets') !== -1) {
                        scope.allSamples = newValue.allSamples;
                        scope.numberOfRows = newValue.numberOfRows;
                        _onSuccess();
                    }
                }, true);

                var _showSummaryStats = function() {
                    template_msg.innerHTML = 'Execute summary statistics, please wait <span class="blink_me">_</span>';
                    rServeService.executeSummaryStats('preprocess', scope.projection).then(
                        function (data) {
                            scope.summaryData = data.result; // this will trigger $watch
                        },
                        _onFail
                    );
                };

                template_btn.onclick = function() {
                    scope.summaryData = {};
                    scope.disabled = true;
                    scope.running = true;
                    template_msg.innerHTML = 'Preprocessing, please wait <span class="blink_me">_</span>';

                    var params = scope.params ? scope.params : {};
                    rServeService.preprocess(params).then(
                        scope.showSummaryStats ? _showSummaryStats : _onSuccess,
                        _onFail
                    );
                };
            }
        };
    }]);
