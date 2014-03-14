/**
 * Retrieve DAS resources from tranSMART's DAS
 *
 * @param track_label
 * @param result_instance_id
 * @returns {Array}
 */
function retrieveTransmartDASSources (node, result_instance_id, callback) {

    if (isHighDimensionalNode(node)) {
        var arrNds = new Array();

        // Code below is hardcoded
        // TODO : Check the local Das features and then collect them in arrNDS

        arrNds[0] = new DASSource({name: 'acgh', uri: pageInfo.basePath + "/das/acgh-" + result_instance_id + "/"});
//        arrNds[1] = new DASSource({name: 'smaf', uri: pageInfo.basePath + "/das/smaf-"+ result_instance_id + "/"});
//        arrNds[2] = new DASSource({name: 'qd', uri: pageInfo.basePath + "/das/qd-" + result_instance_id + "/"});
//        arrNds[3] = new DASSource({name: 'maf', uri: pageInfo.basePath + "/das/maf-"+ result_instance_id + "/"});
//        arrNds[4] = new DASSource({name: 'gv', uri: pageInfo.basePath + "/das/gv-"+ result_instance_id + "/"});
//        arrNds[5] = new DASSource({name: 'vcf', uri: pageInfo.basePath + "/das/vcf-"+ result_instance_id + "/"});

        callback(arrNds);

    } else {
        displayError('Error', 'Cannot display non-High Dimensional node');
    }
}

/**
 * Add track when user drag & drop a node
 * @param node
 * @param result_instance_id_1
 */
Browser.prototype.addTrackByNode = function (node, result_instance_id_1, result_instance_id_2) {

    var thisB = this;
    var das_source;
    var res_inst_id_1, res_inst_id_2;

    // reseting the global subset ids (result instance ids)
    GLOBAL.CurrentSubsetIDs[1] = null;
    GLOBAL.CurrentSubsetIDs[2] = null;

    // get result instance id as representative of cohort selection
    // in Comparison tab
    runAllQueries(function () {

        res_inst_id_1 = GLOBAL.CurrentSubsetIDs[1];
        res_inst_id_2 = GLOBAL.CurrentSubsetIDs[2];

        var knownSpace = thisB.knownSpace;

        // validate space
        if (!knownSpace) {
            alert("Can't confirm track-addition to an unit browser.");
            return;
        }

        var tsm = Math.max(knownSpace.min, (knownSpace.min + knownSpace.max - 100) / 2)|0;
        var testSegment = new DASSegment(knownSpace.chr, tsm, Math.min(tsm + 99, knownSpace.max));


        /** ----------------------------------------------------------- **/
        /** Code below contains callback hell .. TODO: refactor please! **/
        /** ----------------------------------------------------------- **/

        // define features
        var arrNds = retrieveTransmartDASSources(node, res_inst_id_1, function(arr) {

            arr.forEach(function(nds) {

                nds.features(testSegment, {}, function(features) {

                    var nameExtractPattern = new RegExp('/([^/]+)/?$');
                    var match = nameExtractPattern.exec(nds.uri);
                    if (match) {
                        nds.name = match[1];
                    }

                    tryAddDASxSources(nds);

                    return;
                });
            });

        });


        /**
         * Add DAS x Sources
         * @param nds
         * @param retry
         */
        function tryAddDASxSources(nds, retry) {

            var uri = nds.uri;
            if (retry) {
                var match = /(.+)\/[^\/]+\/?/.exec(uri);
                if (match) {
                    uri = match[1] + '/sources';
                }
            }
            function sqfail() {
                if (!retry) {
                    return tryAddDASxSources(nds, true);
                } else {
                    return drawTrack(nds);
                }
            }

            new DASRegistry(uri, {credentials: nds.credentials}).sources(
                function(sources) {
                    if (!sources || sources.length == 0) {
                        return sqfail();
                    }

                    var fs = null;
                    if (sources.length == 1) {
                        fs = sources[0];
                    } else {
                        for (var i = 0; i < sources.length; ++i) {
                            if (sources[i].uri === nds.uri) {
                                fs = sources[i];
                                break;
                            }
                        }
                    }

                    var coordsDetermined = false, quantDetermined = false;

                    if (fs) {
                        nds.name = fs.name;
                        nds.desc = fs.desc;
                        if (fs.maxbins) {
                            nds.maxbins = true;
                        } else {
                            nds.maxbins = false;
                        }
                        if (fs.capabilities) {
                            nds.capabilities = fs.capabilities;
                        }
                        quantDetermined = true

                        if (fs.coords && fs.coords.length == 1) {
                            var coords = fs.coords[0];
                            if (coordsMatch(coords, thisB.coordSystem)) {
                                coordsDetermined = true;
                            } else if (thisB.chains) {
                                for (var k in thisB.chains) {
                                    if (coordsMatch(coords, thisB.chains[k].coords)) {
                                        nds.mapping = k;
                                        coordsDetermined = true;
                                    }
                                }
                            }
                        }

                    }
                    return drawTrack(nds, coordsDetermined, quantDetermined);
                },
                function() {
                    return sqfail();
                }
            );
        }

        /**
         * Draw new track in the swimming lane
         * @param nds
         * @param coordsDetermined
         * @param quantDetermined
         * @param quantIrrelevant
         */
        var drawTrack = function(nds, coordsDetermined, quantDetermined, quantIrrelevant) {

            dataToFinalize = nds;

            var m = '__default__'; // coordinate system
            if (m != '__default__') {
                dataToFinalize.mapping = m;
            } else {
                dataToFinalize.mapping = undefined;
            }
            thisB.sources.push(dataToFinalize);
            thisB.makeTier(dataToFinalize);
        }

    });
}
