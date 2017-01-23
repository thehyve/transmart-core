/**
 * Retrieve DAS resources from tranSMART's DAS
 *
 * @param result_instance_id
 * @returns {Array}
 */

"use strict";

if (typeof(require) !== 'undefined') {
    var browser = require('./cbrowser');
    var Browser = browser.Browser;

    var tUtils = require('./tsmart-utils');
    var displayError = tUtils.displayError;

    var das = require('./das');
    var DASSegment = das.DASSegment;
    var DASRegistry = das.DASRegistry;

}

if (typeof(require) !== 'undefined') {
    var browser = require('./cbrowser');
    var Browser = browser.Browser;
    var sourcesAreEqual = browser.sourcesAreEqual;

    var utils = require('./utils');
    var makeElement = utils.makeElement;
    var removeChildren = utils.removeChildren;
    var Observed = utils.Observed;

    var thub = require('./thub');
    var THUB_COMPARE = thub.THUB_COMPARE;
    var connectTrackHub = thub.connectTrackHub;

    var domui = require('./domui');
    var makeTreeTableSection = domui.makeTreeTableSection;

    var probeResource = require('./probe').probeResource;


    // Most of this could disappear if we leave all probing to the probe module...
    var bin = require('./bin');
    var URLFetchable = bin.URLFetchable;
    var BlobFetchable = bin.BlobFetchable;
    var readInt = bin.readInt;

    var lh3utils = require('./lh3utils');
    var unbgzf = lh3utils.unbgzf;

    var bam = require('./bam');
    var BAM_MAGIC = bam.BAM_MAGIC;
    var BAI_MAGIC = bam.BAI_MAGIC;

    var tbi = require('./tabix');
    var TABIX_MAGIC = tbi.TABIX_MAGIC;


    var das = require('./das');
    var DASSource = das.DASSource;
    var DASSegment = das.DASSegment;
    var DASRegistry = das.DASRegistry;
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

    /**
     * Get tranSMART's DAS source
     * @param result_instance_id
     * @param dataType
     * @returns {Array}
     * @private
     */
    var _getTransmartDASSources = function (result_instance_id, concept_key, dataType) {

        var arrNds = new Array();

        // Result instance id and concept key will be included in uri
        var encoded_concept_key = btoa(concept_key);
        var das_suffix = "" + result_instance_id;
        if (encoded_concept_key) {
            das_suffix += "-" + encoded_concept_key;
        }

        if (dataType == 'acgh') {
            arrNds[0] = new DASSource({name: 'acgh-gain', uri: pageInfo.basePath + "/das/acgh-gain-" + das_suffix + "/"});
            arrNds[1] = new DASSource({name: 'acgh-loss', uri: pageInfo.basePath + "/das/acgh-loss-" + das_suffix + "/"});
            arrNds[2] = new DASSource({name: 'acgh-normal', uri: pageInfo.basePath + "/das/acgh-normal-" + das_suffix + "/"});
            arrNds[3] = new DASSource({name: 'acgh-amp', uri: pageInfo.basePath + "/das/acgh-amp-" + das_suffix + "/"});
            arrNds[4] = new DASSource({name: 'acgh-inv', uri: pageInfo.basePath + "/das/acgh-inv-" + das_suffix + "/"});
        } else if (dataType == 'vcf') {
            arrNds[0] = new DASSource({name: 'smaf', uri: pageInfo.basePath + "/das/smaf-"+ das_suffix + "/"});
            arrNds[1] = new DASSource({name: 'qd', uri: pageInfo.basePath + "/das/qd-" + das_suffix + "/"});
            arrNds[3] = new DASSource({name: 'gv', uri: pageInfo.basePath + "/das/gv-"+ das_suffix + "/"});
            arrNds[4] = new DASSource({name: 'vcf', uri: pageInfo.basePath + "/das/vcf-"+ das_suffix + "/"});
        } else {
            console.log("Unknown data type", dataType);
        }

        return arrNds;
    }

    /**
     * Add DAS source
     * @param arr
     * @param nameSuffix
     * @param testSegment
     * @param tryAddDASxSources
     * @param mapping
     * @private
     */
    var _addDasSource = function (arr, nameSuffix, testSegment, tryAddDASxSources, mapping) {

        arr.forEach(function(nds) {

            nds.features(testSegment, {}, function(features) {

                if (!nds.name)  {
                    var nameExtractPattern = new RegExp('/([^/]+)/?$');
                    var match = nameExtractPattern.exec(nds.uri);
                    if (match) {
                        nds.name = match[1];
                    }
                }

                if (mapping) {
                    nds.mapping = mapping;
                }

                tryAddDASxSources(nds, nameSuffix);

                return;
            });
        });

    }

    // reseting the global subset ids (result instance ids)
    GLOBAL.CurrentSubsetIDs[1] = null;
    GLOBAL.CurrentSubsetIDs[2] = null;

    var _getNodeDetails = function (node, callback) {

        var nodReq = new XMLHttpRequest();
        var nodUrl = pageInfo.basePath + "/HighDimension/nodeDetails";

        nodReq.open('POST', nodUrl, true);
        nodReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        nodReq.onreadystatechange = function() {
            if (nodReq.readyState == 4) {
                if (nodReq.status == 200 || nodReq.status == 206) {
                    return callback(nodReq.responseText);
                } else {
                    return callback(null);
                }
            }
        };

        var params = "conceptKeys="+encodeURIComponent([node.id]);
        nodReq.send(params);

    };

    var _getGenomeBuildMapping = function (genomeReleaseId) {
        // If the browser's coordinate system doesn't match the genome
        // release that we got from the node details, check if there is
        // a mapping (chain) available
        if (thisB.coordSystem.compatibleIds.indexOf(genomeReleaseId) == -1) {
            var defaultCoords = thisB.coordSystem.auth + thisB.coordSystem.version +
                "/" + thisB.coordSystem.ucscName;

            if (thisB.chains) {
                for (var mapping in thisB.chains) {
                    var compatibleIds = thisB.chains[mapping].coords.compatibleIds;
                    if (compatibleIds.indexOf(genomeReleaseId) >= 0) {
                        alert("The newly added tracks will be mapped from genome release '" +
                            genomeReleaseId + "' to '" + defaultCoords + "' using mapping '" +
                            mapping + "'");
                        return mapping;
                    }
                }
            }

            // Warn if we couldn't find a coordinate mapping
            if (genomeReleaseId) {
                alert("Could not find a coordinate mapping for the genome release " +
                    "specified in the data (" + genomeReleaseId + ") to the genome " +
                    "browser's coordinate system (" + defaultCoords + "). " +
                    "If the coordinate systems differ, displayed information will be incorrect.");
            }
            else {
                alert("No genome release version specified for the selected data. " +
                    "Make sure it was specified correctly at upload in the platform (gpl_info). " +
                    "Newly added tracks will be displayed as if they were in the genome browser's coordinate system " +
                    "(" + defaultCoords + "). The displayed information might be incorrect.");
            }
        }
        return "";
    }

    // get result instance id as representative of cohort selection
    // in Comparison tab
    runAllQueries(function () {

        res_inst_id_1 = GLOBAL.CurrentSubsetIDs[1];
        res_inst_id_2 = GLOBAL.CurrentSubsetIDs[2];

        var knownSpace = thisB.knownSpace;

        /**
         * Check if a node is high dimensional or not
         * @param node
         * @returns {boolean}
         */
        var _isHighDimensionalNode = function (node) {
            if (node.attributes.visualattributes.indexOf('HIGH_DIMENSIONAL') != -1)
                return true;
            else
                return false;
        }

        // validate space
        if (!knownSpace) {
            alert("Can't confirm track-addition to an unit browser.");
            return;
        }

        var tsm = Math.max(knownSpace.min, (knownSpace.min + knownSpace.max - 100) / 2)|0;
        var testSegment = new DASSegment(knownSpace.chr, tsm, Math.min(tsm + 99, knownSpace.max));

        var _nodeDetails = _getNodeDetails(node, function (response) {

            // Get datatype and genome release id
            var dataType = "";
            var genomeReleaseId = "";
            var details = JSON.parse(response);
            for (var key in details) {
                dataType = key;
                var platform = details[key].platforms[0];
                if (platform.genomeReleaseId) {
                    genomeReleaseId = platform.genomeReleaseId.split(".")[0];
                }
            }

            if (_isHighDimensionalNode(node)) {
                // Get the mapping to apply and show warnings if appropriate
                var mapping = _getGenomeBuildMapping(genomeReleaseId);

                // define features
                var sources = _getTransmartDASSources(res_inst_id_1, node.id, dataType);
                _addDasSource(sources, res_inst_id_2 ? '-subset 1' : '', testSegment, tryAddDASxSources, mapping);
                if (res_inst_id_2) {
                    sources = _getTransmartDASSources(res_inst_id_2, node.id, dataType);
                    _addDasSource(sources, '-subset 2', testSegment, tryAddDASxSources, mapping);
                }
                thisB.createAddInfoButton()
            } else {
                displayError('Error', 'Cannot display non-High Dimensional node');
            }

        });

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

                    }
                    return drawTrack(nds);
                },
                function() {
                    return sqfail();
                }
            );
        }

        /**
         * Draw new track in the swimming lane
         * @param nds
         */
        var drawTrack = function(nds) {
            thisB.sources.push(nds);
            thisB.makeTier(nds);
        }

    });
}

Browser.prototype.createAddInfoButton= function() {
    var that = this;
    //get the Add track button
    var dalBtns = jQuery('.pull-right.btn-group').children();
    jQuery(dalBtns[0]).click(function() {
        if (jQuery('#btnAddVCFINFO').length > 0)
            return; //it's already there
        //add a button to add custom INFO tracks for VCF
        var btn = that.makeButton('Add VCF INFO', 'Add a custom track with a particular field from the INFO column in a VCF file');
        btn.id = 'btnAddVCFINFO';
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
                    that.addVCFInfoTrack(infoField, vcfs[0].id, vcfs[0].concept, vcfs.length>1?'-subset 1' : '');
                    if (vcfs.length>1)
                        that.addVCFInfoTrack(infoField, vcfs[1].id, vcfs[1].concept, '-subset 2');
                }
            }
            else {
                alert('Please, first drop a VCF node from the concept tree on the genome browser.');
            }
        });
        jQuery('.nav').prepend(btn);
    })
}

Browser.prototype.addVCFInfoTrack= function(infoField, qri, concept, nameSuffix) {
    this.addTier(new DASSource({
        name: 'VCF-'+infoField.trim()+nameSuffix,
        uri: pageInfo.basePath + "/das/vcfInfo-" + infoField.trim() +
            '-' + qri + '-' + concept + "/"
    }))
}

Browser.prototype.scanCurrentTracksForVCF = function () {
    var subset1, subset2;
    var subset1INFOs=[], subset2INFOs=[];
    var vcfs = [];
    for (var i = 0; i < this.sources.length; i++){
        var s = this.sources[i];
        if (!s.uri) continue;
        if (s.uri.indexOf('vcf-') > -1 ||
            s.uri.indexOf('smaf-') > -1 ||
            s.uri.indexOf('qd-') > -1 ||
            s.uri.indexOf('gv-') > -1) {
            //stuff between last / identifies track, numbers after last - identify QRI
            var match = /([^\-]+)-([^\-]+)-([^\/]+)\/$/.exec(s.uri);
            if (match) {
                var qri = match[2];
                var concept = match[3];

                var vcfEntry = null;
                for (var j = 0; j < vcfs.length; j++) {
                    if (vcfs[j].id == qri && vcfs[j].concept == concept) {
                        vcfEntry = vcfs[j];
                    }
                }
                if (vcfEntry == null) {
                    vcfEntry = {id: qri, concept: concept};
                    vcfs.push(vcfEntry);
                }
            }
        }
    }
    return vcfs;
}
