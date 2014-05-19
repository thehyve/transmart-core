/**
 * forEach is a recent addition to the ECMA-262 standard; as such it may not be present in other implementations of the
 * standard. You can work around this by inserting the following code at the beginning of your scripts, allowing use of
 * forEach in implementations which do not natively support it.
 *
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/forEach
 */

if (!Array.prototype.forEach) {
    Array.prototype.forEach = function (fn, scope) {
        'use strict';
        var i, len;
        for (i = 0, len = this.length; i < len; ++i) {
            if (i in this) {
                fn.call(scope, this[i], i, this);
            }
        }
    };
}

/**
 * Load Dalliance Genome Browser
 * @param resultsTabPanel
 */
function loadDalliance(resultsTabPanel) {
    // everything starts here ..
    resultsTabPanel.add(genomeBrowserPanel);
    resultsTabPanel.doLayout();
}

/**
 * Create new instance of Genome Browser Panel
 * @type {Ext.Panel}
 */
var genomeBrowserPanel = new Ext.Panel(
    {
        id: 'dallianceBrowser',
        pluginName: 'dalliance-plugin',
        title: 'Genome Browser',
        region: 'center',
        split: true,
        height: 90,
        layout: 'fit',
        bodyStyle: 'display:flex;display:-webkit-flex;width: 100%',
        autoLoad: {
            url: pageInfo.basePath + "/Dalliance/index"
        },
        genomeBrowser: null,
        listeners: {
            activate: function (p) {
                // create genome browser instance
                // when this panel is activated,
                // or reset if it already exists
                if (this.genomeBrowser == null) {
                    this.createGenomeBrowser();
                }
                else {
                    this.genomeBrowser.resizeViewer();
                }
            },
            afterLayout: function () {
                // make this panel as droppable area
                // after being drawn
                this.applyDroppable();
            }
        },
        collapsible: true,

        applyDroppable: function () {
            var dropTarget = this.getEl().select('.x-panel-bwrap .x-panel-body'); //return array
            var _this = this;
            var ddTarget = new Ext.dd.DropTarget(dropTarget.elements[0], {
                    ddGroup: 'makeQuery',
                    notifyDrop: this.notifyDrop,
                    parent: this
                }
            );
        },

        notifyDrop: function (source, e, data) {

            var result_instance_id_1 = GLOBAL.CurrentSubsetIDs[1];
            var result_instance_id_2 = GLOBAL.CurrentSubsetIDs[2];

            // add track in the genome browser
            var b = this.parent.genomeBrowser;
            b.addTrackByNode(data.node, result_instance_id_1, result_instance_id_2);
            //this.parent.genomeBrowser.showTrackAdder(e);
        },

        // create new instance of dalliance browser
        createGenomeBrowser: function () {

            this.genomeBrowser = new Browser({
                chr:          '22',
                viewStart:    30000000,
                viewEnd:      30030000,
                cookieKey:    'human-grc_h37',

                coordSystem: {
                    speciesName: 'Human',
                    taxon: 9606,
                    auth: 'GRCh',
                    version: '37',
                    ucscName: 'hg19'
                },

                chains: {
                    hg18ToHg19: new Chainset('http://www.derkholm.net:8080/das/hg18ToHg19/', 'NCBI36', 'GRCh37',
                        {
                            speciesName: 'Human',
                            taxon: 9606,
                            auth: 'GRCh',
                            version: 36
                        })
                },

                sources:     [{name:                 'Genome',
                    twoBitURI:            'http://www.biodalliance.org/datasets/hg19.2bit',
                    tier_type:            'sequence',
                    provides_entrypoints: true,
                    pinned: true
                },
                    {name: 'GENCODE',
                        bwgURI: 'http://www.biodalliance.org/datasets/gencode.bb',
                        stylesheet_uri: 'http://www.biodalliance.org/stylesheets/gencode.xml',
                        collapseSuperGroups: true,
                        trixURI: 'http://www.biodalliance.org/datasets/geneIndex.ix'},
                    {name: 'Repeats',
                        desc: 'Repeat annotation from RepeatMasker',
                        bwgURI: 'http://www.biodalliance.org/datasets/repeats.bb',
                        stylesheet_uri: 'http://www.biodalliance.org/stylesheets/bb-repeats.xml',
                        forceReduction: -1},
                    {name: 'SNPs',
                        tier_type: 'ensembl',
                        species:'human',
                        type: 'variation',
                        disabled: true,
                        featureInfoPlugin: function(f, info) {
                            if (f.id) {
                                info.add('SNP', makeElement('a', f.id, {href: 'http://www.ensembl.org/Homo_sapiens/Variation/Summary?v=' + f.id, target: '_newtab'}));
                            }
                        }
                    },
                    {name: 'CpG',
                        desc: 'CpG observed/expected ratio',
                        uri: 'http://www.derkholm.net:8080/das/hg19comp/',
                        // stylesheet_uri: 'http://www.derkholm.net/dalliance-test/stylesheets/cpg.xml'
                        quantLeapThreshold: 0.8,
                        forceReduction: -1,
                        style:                [{type: 'cpgoe',
                            style: {glyph: 'LINEPLOT',
                                FGCOLOR: 'green', HEIGHT: '50', MIN: 0, MAX: 1.2}}]
                    },
                    {name:                 'BWG test',
                        bwgURI:               'http://www.biodalliance.org/datasets/spermMethylation.bw',
                        stylesheet_uri:       'http://www.ebi.ac.uk/das-srv/genomicdas/das/batman_seq_SP/stylesheet',
                        mapping:              'hg18ToHg19',
                        quantLeapThreshold: 80
                    }
                ],

                setDocumentTitle: true,
                uiPrefix: '',

                fullScreen: true,

                browserLinks: {
                    Ensembl: 'http://ncbi36.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',
                    UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',
                    Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'
                }
            });

            this.genomeBrowser.hubs = [
                'http://www.biodalliance.org/datasets/testhub/hub.txt',
                'http://ftp.ebi.ac.uk/pub/databases/ensembl/encode/integration_data_jan2011/hub.txt'
            ];

            this.genomeBrowser.addFeatureInfoPlugin(function(f, info) {
                info.add('Testing', 'This is a test!');
            });

            this.genomeBrowser.addViewListener(function(chr, min, max) {
                var link = document.getElementById('enslink');
                link.href = 'http://www.ensembl.org/Homo_sapiens/Location/View?r=' + chr + ':' + min + '-' + max;
            });

            /*
             var geneDescriptions;
             connectBigTab(new URLFetchable('http://www.biodalliance.org/datasets/ensg-to-desc.bt'), function(bt) {
             geneDescriptions = bt;
             });

             b.addFeatureInfoPlugin(function(f, info) {
             if (f.geneId) {
             var desc = makeElement('div', 'Loading...');
             info.add('Description', desc);
             geneDescriptions.index.lookup(f.geneId, function(res, err) {
             if (err) {
             console.log(err);
             } else {
             desc.textContent = res;
             }
             });
             }
             }); */


/*
            this.genomeBrowser = new Browser({
                chr:                 '22',
                viewStart:           30700000,
                viewEnd:             30900000,
                cookieKey:           'human-grc_h37',

                coordSystem: {
                    speciesName: 'Human',
                    taxon: 9606,
                    auth: 'GRCh',
                    version: '37',
                    ucscName: 'hg19'
                },

                chains: {
                    hg18ToHg19: new Chainset('http://www.derkholm.net:8080/das/hg18ToHg19/', 'NCBI36', 'GRCh37',
                        {
                            speciesName: 'Human',
                            taxon: 9606,
                            auth: 'NCBI',
                            version: 36,
                            ucscName: 'hg18'
                        })
                },

                sources: [
                    {name: 'Genome',
                    twoBitURI: 'http://www.biodalliance.org/datasets/hg19.2bit',
                    tier_type: 'sequence'},
                    {name: 'Genes',
                        desc: 'Gene structures from GENCODE 19',
                        bwgURI: 'http://www.biodalliance.org/datasets/gencode.bb',
                        stylesheet_uri: 'http://www.biodalliance.org/stylesheets/gencode.xml',
                        collapseSuperGroups: true,
                        trixURI: 'http://www.biodalliance.org/datasets/geneIndex.ix'}
//                    {name: 'Repeats',
//                        desc: 'Repeat annotation from Ensembl 59',
//                        bwgURI: 'http://www.biodalliance.org/datasets/repeats.bb',
//                        stylesheet_uri: 'http://www.biodalliance.org/stylesheets/bb-repeats.xml'},
//                    {name: 'Conservation',
//                        desc: 'Conservation',
//                        bwgURI: 'http://www.biodalliance.org/datasets/phastCons46way.bw',
//                        noDownsample: true},
//                    {name: 'GM12878 ChromHMM', desc: 'GM12878 ChromHMM Genome Segmentation',
//                        pennant: 'http://genome.ucsc.edu/images/encodeThumbnail.jpg',
//                        bwgURI: 'http://ftp.ebi.ac.uk/pub/databases/ensembl/encode/integration_data_jan2011/byDataType/segmentations/jan2011/hub/gm12878.ChromHMM.bb',
//                        style: [{type: 'bigwig', style: {glyph: 'BOX', FGCOLOR: 'black', BGCOLOR: 'blue', HEIGHT: 8, BUMP: false, LABEL: false, ZINDEX: 20, BGITEM: true, id: 'style1'}},
//                            {type: 'bb-translation', style: {glyph: 'BOX', FGCOLOR: 'black', BGITEM: true, BGCOLOR: 'red', HEIGHT: 10, BUMP: true, ZINDEX: 20, id: 'style2'}},
//                            {type: 'bb-transcript', style: {glyph: 'BOX', FGCOLOR: 'black', BGCOLOR: 'white', HEIGHT: 10, ZINDEX: 10, BUMP: true, LABEL: true, id: 'style3'}}]}
                ],

                setDocumentTitle: true,

                browserLinks: {
                    Ensembl: 'http://www.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',
                    UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',
                    Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'
                },

                hubs: ['http://ftp.ebi.ac.uk/pub/databases/ensembl/encode/integration_data_jan2011/hub.txt']
            });
*/

//            var that = this;
//            setTimeout(function() {
//                that.genomeBrowser.realInit();
////                //if we get rid of max-height, dalliance browser is able to fill the screen
////                jQuery('.dalliance-root').css('max-height', 'none');
////                setTimeout(function() {that.genomeBrowser.resizeViewer();},0);
//
//                var vcfs = that.genomeBrowser.scanCurrentTracksForVCF();
//
//                if (vcfs.length>0) {
//                    that.genomeBrowser.createAddInfoButton();
//                }
//
//            }, 0);

        }
    }
);

