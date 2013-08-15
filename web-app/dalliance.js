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
        id : 'dallianceBrowser',
        pluginName : 'dalliance-plugin',
        title : 'Genome Browser',
        region : 'center',
        split : true,
        height : 90,
        layout : 'fit',
        autoLoad : {
            url : pageInfo.basePath+"/Dalliance/index"
        },
        genomeBrowser : null,
        listeners :
        {
            activate : function(p) {
                // create genome browser instance
                // when this panel is activated
                this.createGenomeBrowser();
            },
            afterLayout : function () {
                // make this panel as droppable area
                // after being drawn
                this.applyDroppable();
            }
        },
        collapsible : true,

        applyDroppable: function() {
            var dropTarget = this.getEl().select('.x-panel-bwrap .x-panel-body'); //return array
            var _this = this;
            var ddTarget = new Ext.dd.DropTarget(dropTarget.elements[0], {
                    ddGroup: 'makeQuery',
                    notifyDrop: this.notifyDrop,
                    parent: this
                }
            );
        },

        notifyDrop: function(source, e, data) {

            // get selected concept from the dropped node
            var concept = convertNodeToConcept(data.node);

            var result_instance_id_1 = GLOBAL.CurrentSubsetIDs[1];
            var result_instance_id_2 = GLOBAL.CurrentSubsetIDs[2];

            // add track in the genome browser
            var b = this.parent.genomeBrowser;
            b.addTrackByNode(concept, result_instance_id_1, result_instance_id_2);
            //this.parent.genomeBrowser.showTrackAdder(e);
        },

        // create new instance of dalliance browser
        createGenomeBrowser : function () {
            this.genomeBrowser = new Browser({
                chr:          '22',
                viewStart:    30096000,
                viewEnd:      30276000,
                cookieKey:    'human2',

                chains: {
                    hg19ToHg18: new Chainset('http://www.derkholm.net:8080/das/hg19ToHg18/', 'GRCh37', 'NCBI36',
                        {
                            speciesName: 'Human',
                            taxon: 9606,
                            auth: 'GRCh',
                            version: 37
                        })
                },

                sources:     [
                    {
                        name:                 'Genome',
                        twoBitURI:            'http://www.biodalliance.org/datasets/hg18.2bit',
                        // uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                        tier_type:            'sequence'
                    },
                    {
                        name: 'MeDIP-raw',
                        desc: 'MeDIP-seq reads from Nature Biotech. 26:779-785',
                        uri: 'http://www.derkholm.net:8080/das/medipseq_reads/'
                    },
                    {
                        name:                 'Genes',
                        desc:                 'Gene structures from Ensembl 54',
                        uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                        collapseSuperGroups:  true,
                        provides_karyotype:   true,
                        provides_search:      true,
                        provides_entrypoints: true,
                        maxbins:              false
                    },
                    {
                        name:                 'Repeats',
                        uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                        stylesheet_uri:       'http://www.biodalliance.org/stylesheets/repeats-L1.xml'
                    },
                    {
                        name:                 'CpG Density',
                        uri:                  'http://www.derkholm.net:8080/das/hg18comp/',
                        // stylesheet_uri:       'http://www.biodalliance.org/stylesheets/cpg-hist.xml',
                        style:                [{type: 'cpgoe',style: {glyph: 'LINEPLOT', FGCOLOR: 'green', HEIGHT: '50'}}]
                    }

                ],

                setDocumentTitle: true,

                searchEndpoint: new DASSource('http://www.derkholm.net:8080/das/hsa_54_36p/'),

                browserLinks: {
                    Ensembl: 'http://ncbi36.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',
                    UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg18&position=chr${chr}:${start}-${end}',
                    Sequence: 'http://www.derkholm.net:8080/das/hg18comp/sequence?segment=${chr}:${start},${end}'
                }
            });
        }
    }
);

