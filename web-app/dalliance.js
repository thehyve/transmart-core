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
        bodyStyle: 'display:flex;display:-webkit-flex;',
        autoLoad: {
            url: pageInfo.basePath + "/Dalliance/index"
        },
        genomeBrowser: null,
        listeners: {
            activate: function (p) {
                // create genome browser instance
                // when this panel is activated
                this.createGenomeBrowser();
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
                        trixURI: 'http://www.biodalliance.org/datasets/geneIndex.ix'},
                    {name: 'Repeats',
                        desc: 'Repeat annotation from Ensembl 59',
                        bwgURI: 'http://www.biodalliance.org/datasets/repeats.bb',
                        stylesheet_uri: 'http://www.biodalliance.org/stylesheets/bb-repeats.xml'},
                    {name: 'Conservation',
                        desc: 'Conservation',
                        bwgURI: 'http://www.biodalliance.org/datasets/phastCons46way.bw',
                        noDownsample: true},
                    {name: 'GM12878 ChromHMM', desc: 'GM12878 ChromHMM Genome Segmentation',
                        pennant: 'http://genome.ucsc.edu/images/encodeThumbnail.jpg',
                        bwgURI: 'http://ftp.ebi.ac.uk/pub/databases/ensembl/encode/integration_data_jan2011/byDataType/segmentations/jan2011/hub/gm12878.ChromHMM.bb',
                        style: [{type: 'bigwig', style: {glyph: 'BOX', FGCOLOR: 'black', BGCOLOR: 'blue', HEIGHT: 8, BUMP: false, LABEL: false, ZINDEX: 20, BGITEM: true, id: 'style1'}},
                            {type: 'bb-translation', style: {glyph: 'BOX', FGCOLOR: 'black', BGITEM: true, BGCOLOR: 'red', HEIGHT: 10, BUMP: true, ZINDEX: 20, id: 'style2'}},
                            {type: 'bb-transcript', style: {glyph: 'BOX', FGCOLOR: 'black', BGCOLOR: 'white', HEIGHT: 10, ZINDEX: 10, BUMP: true, LABEL: true, id: 'style3'}}]}
                ],

                setDocumentTitle: true,

                browserLinks: {
                    Ensembl: 'http://www.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',
                    UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',
                    Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'
                },

                hubs: ['http://ftp.ebi.ac.uk/pub/databases/ensembl/encode/integration_data_jan2011/hub.txt']
            });

            var that = this;
            setTimeout(function() {
                that.genomeBrowser.realInit();
                //if we get rid of max-height, dalliance browser is able to fill the screen
                jQuery('.dalliance-root').css('max-height', 'none');

                //get the Add track button
                var dalBtns = jQuery('.pull-right.btn-group').children();
                jQuery(dalBtns[0]).click(function() {
                    //add a button to add custom INFO tracks for VCF
                    var btn = that.genomeBrowser.makeButton('Add VCF INFO', 'Add a custom track with a particular field from the INFO column in a VCF file');
                    btn.addEventListener('click', function(ev) {
                        ev.preventDefault(); ev.stopPropagation();
                        //only add the track if there is a query result instance
                        if (GLOBAL.CurrentSubsetIDs[1]) {
                            var infoField = prompt(
                                'You can add custom track from the INFO column. If you know the VCF file\'s INFO column contains for example: \n\nDP=89;AF1=1;AC1=2;DP4=0,0,81,0;MQ=60;FQ=-271,\n\n you can add a track for DP to see the values of DP plotted. \n'+
                                'Please, first drop a VCF node from the concept tree on the genome browser. \n'+
                                'Note: please remember to remove the track and add it again if you change the patient subset selection criteria',
                                'DP');
                            if (infoField != null) {
                                var result_instance_id = GLOBAL.CurrentSubsetIDs[1];
                                that.genomeBrowser.addTier(new DASSource({
                                    name: 'VCF-'+infoField.trim(),
                                    uri: pageInfo.basePath + "/das/vcfInfo-"+infoField.trim()+'-'+ result_instance_id + "/"
                                }))
                            }
                        }
                        else {
                            alert('Please, first drop a VCF node from the concept tree on the genome browser.');
                        }
                    });
                    jQuery('.nav').prepend(btn);
                })
            }, 0);
        }
    }
);

