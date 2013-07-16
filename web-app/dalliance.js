/**
 * panel to display gnome browser
 *
 * @type {Ext.Panel}
 */


function loadDalliance(resultsTabPanel) {

    // everything starts here ..
    resultsTabPanel.add(dallianceBrowser);
    resultsTabPanel.doLayout();

}

dallianceBrowser = new Ext.Panel(
    {
        id : 'dallianceBrowser',
        pluginName : 'dalliance-plugin',
        title : 'Dalliance Browser',
        region : 'center',
        split : true,
        height : 90,
        layout : 'fit',
        autoLoad : {
            url : pageInfo.basePath+"/Dalliance/index"
        },
        listeners :
        {
            activate : function(p) {
                var b = new Browser({
                    chr:          '22',
                    viewStart:    30000000,
                    viewEnd:      30030000,
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

                    sources:     [{name:                 'Genome',
                        twoBitURI:            'http://www.biodalliance.org/datasets/hg18.2bit',
                        // uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                        tier_type:            'sequence'},
                        {name:                 'Genes',
                            desc:                 'Gene structures from Ensembl 54',
                            uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                            collapseSuperGroups:  true,
                            provides_karyotype:   true,
                            provides_search:      true,
                            provides_entrypoints: true,
                            maxbins:              false},
                        {name:                 'Repeats',
                            uri:                  'http://www.derkholm.net:8080/das/hsa_54_36p/',
                            stylesheet_uri:       'http://www.biodalliance.org/stylesheets/repeats-L1.xml'},
                        {name:                 'CpG Density',
                            uri:                  'http://www.derkholm.net:8080/das/hg18comp/',
                            // stylesheet_uri:       'http://www.biodalliance.org/stylesheets/cpg-hist.xml',
                            style:                [{type: 'cpgoe',
                                style: {glyph: 'LINEPLOT',
                                    FGCOLOR: 'green', HEIGHT: '50'}}]},
                        {name: 'MeDIP-raw',
                            desc: 'MeDIP-seq reads from Nature Biotech. 26:779-785',
                            uri: 'http://www.derkholm.net:8080/das/medipseq_reads/'},
                        {name:                 'BWG test',
                            bwgURI:               'http://www.biodalliance.org/datasets/spermMethylation.bw',
                            stylesheet_uri:       'http://www.ebi.ac.uk/das-srv/genomicdas/das/batman_seq_SP/stylesheet'},
                        {name:                 'BBD test',
                            bwgURI:               'http://www.biodalliance.org/datasets/ensGene.bb',
                            link:                 'http://ncbi36.ensembl.org/Homo_sapiens/Gene/Summary?t=$$',
                            collapseSuperGroups:  true,
                            disable: true},
                        {name:                 'Style test',
                            uri:                  pageInfo.basePath+'/glyph-test/',
                            features_uri:         pageInfo.basePath+'/glyph-test/features.xml',
                            stylesheet_uri:       pageInfo.basePath+'/glyph-test/stylesheet.xml'}
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
        },
        collapsible : true
    }
);
