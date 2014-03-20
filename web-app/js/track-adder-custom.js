/**
 * Retrieve DAS resources from tranSMART's DAS
 *
 * @param result_instance_id
 * @returns {Array}
 */
function getTransmartDASSources (result_instance_id) {

        var arrNds = new Array();

        // Code below is hardcoded
        // TODO : Check the local Das features and then collect them in arrNDS

        arrNds[0] = new DASSource({name: 'acgh', uri: pageInfo.basePath + "/das/acgh-" + result_instance_id + "/"});
        arrNds[1] = new DASSource({name: 'smaf', uri: pageInfo.basePath + "/das/smaf-"+ result_instance_id + "/"});
        arrNds[2] = new DASSource({name: 'qd', uri: pageInfo.basePath + "/das/qd-" + result_instance_id + "/"});
//        arrNds[3] = new DASSource({name: 'maf', uri: pageInfo.basePath + "/das/maf-"+ result_instance_id + "/"});
        arrNds[3] = new DASSource({name: 'gv', uri: pageInfo.basePath + "/das/gv-"+ result_instance_id + "/"});
        arrNds[4] = new DASSource({name: 'vcf', uri: pageInfo.basePath + "/das/vcf-"+ result_instance_id + "/"});

        return arrNds;
}

function addDasSource(arr, nameSuffix, testSegment, tryAddDASxSources) {

    arr.forEach(function(nds) {

        nds.features(testSegment, {}, function(features) {

            if (!nds.name)  {
                var nameExtractPattern = new RegExp('/([^/]+)/?$');
                var match = nameExtractPattern.exec(nds.uri);
                if (match) {
                    nds.name = match[1];
                }
            }

            tryAddDASxSources(nds, nameSuffix);

            return;
        });
    });

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




        if (isHighDimensionalNode(node)) {
            // define features
            var sources = getTransmartDASSources(res_inst_id_1);
            addDasSource(sources, res_inst_id_2 ? '-subset 1' : '', testSegment, tryAddDASxSources);
            if (res_inst_id_2) {
                sources.concat(getTransmartDASSources(res_inst_id_2));
                addDasSource(sources, '-subset 2', testSegment, tryAddDASxSources);
            }
        } else {
            displayError('Error', 'Cannot display non-High Dimensional node');
        }


        /**
         * Add DAS x Sources
         * @param nds
         * @param nameSuffix for distinguishing multiple subsets
         * @param retry
         */
        function tryAddDASxSources(nds, nameSuffix, retry) {

            var uri = nds.uri;
            if (retry) {
                var match = /(.+)\/[^\/]+\/?/.exec(uri);
                if (match) {
                    uri = match[1] + '/sources';
                }
            }
            function sqfail() {
                if (!retry) {
                    return tryAddDASxSources(nds, nameSuffix, true);
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
                        nds.name = fs.name+nameSuffix;
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

Browser.prototype.createAddInfoButton= function() {
    that = this;
    //get the Add track button
    var dalBtns = jQuery('.pull-right.btn-group').children();
    jQuery(dalBtns[0]).click(function() {
        //add a button to add custom INFO tracks for VCF
        var btn = that.makeButton('Add VCF INFO', 'Add a custom track with a particular field from the INFO column in a VCF file');
        btn.addEventListener('click', function(ev) {
            ev.preventDefault(); ev.stopPropagation();
            var vcfs = that.scanCurrentTracksForVCF();

            //only add the track if there is a query result instance
            if (vcfs.length > 0) {
                var infoField = prompt(
                    'You can add custom track from the INFO column. If you know the VCF file\'s INFO column contains for example: \n\nDP=89;AF1=1;AC1=2;DP4=0,0,81,0;MQ=60;FQ=-271,\n\n you can add a track for DP to see the values of DP plotted. \n'+
                        'Please, first drop a VCF node from the concept tree on the genome browser. \n'+
                        'Note: please remember to remove the track and add it again if you change the patient subset selection criteria',
                    'DP');
                if (infoField != null) {
                    that.addVCFInfoTrack(infoField, vcfs[0].id, vcfs.length>1?'-subset 1' : '');
                    if (vcfs.length>1)
                        that.addVCFInfoTrack(infoField, vcfs[1].id, '-subset 2');
                }
            }
            else {
                alert('Please, first drop a VCF node from the concept tree on the genome browser.');
            }
        });
        jQuery('.nav').prepend(btn);
    })
}

Browser.prototype.addVCFInfoTrack= function(infoField, qri, nameSuffix) {
    this.addTier(new DASSource({
        name: 'VCF-'+infoField.trim()+nameSuffix,
        uri: pageInfo.basePath + "/das/vcfInfo-"+infoField.trim()+'-'+ qri + "/"
    }))
}

Browser.prototype.scanCurrentTracksForVCF = function () {
    var subset1, subset2; var subset1INFOs=[], subset2INFOs=[];
    var vcfs = [];
    for (var i=0;i<this.sources.length;i++){
        var s = this.sources[i];
        if (!s.uri) continue;
        if (s.uri.indexOf('vcf-') > -1 ||
            s.uri.indexOf('vcfInfo-') > -1 ||
            s.uri.indexOf('smaf-') > -1 ||
            s.uri.indexOf('qd-') > -1 ||
            s.uri.indexOf('gv-') > -1) {
            //stuff between last / identifies track, numbers after last - identify QRI
            var match = /([^\-]+)-(([^\-]+)-)?([^\/]+)\/$/.exec(s.uri)
            if (match) {
                var qri = match[4]; //QRI is fourth group
                var info = match[3];
                var subset = null;
                for (var j=0;j<vcfs.length;j++)
                    if (vcfs[j].id == qri)
                        subset = vcfs[j];
                if (subset == null)
                    vcfs.push(subset = {id: qri, infos:[info], sources: [s]});
                else {
                    subset.infos.push(info);
                    subset.sources.push(s);
                }

            }
        }
    }
    return vcfs;
}