//# sourceURL=ppmidemo.js

'use strict';

window.smartRApp.controller('PPMIDemoController',
    ['$scope', 'smartRUtils', 'commonWorkflowService', function($scope, smartRUtils, commonWorkflowService) {

        commonWorkflowService.initializeWorkflow('ppmidemo', $scope);

        $scope.variantDB = {
            data: [],
            file: '',
            regions: '',
            selectedGenes: '',
            server: "http://10.79.2.77/accessDB",
            func_refgene: {
                exonic: false,
                intronic: false,
                upstream: false,
                downstream: false,
                splicing: false,
                intergenetic: false
            },
            exonicfunc_refgene: {
                frameshift_insertion: false,
                nonframeshift_insertion: false,
                frameshift_deletion: false,
                nonframeshift_deletion: false,
                frameshift_substitution: false,
                nonframeshift_substitution: false,
                synonymous_SNV: false,
                nonsynonymous_SNV: false,
            },
            misc: {
                globalMAF: 0.1,
                cohortMAF: 0.5
            },
            invalid: false,
            running: false
        };

        $scope.pdmap = {
            user: "test",
            password: "test.123",
            server: "http://pg-sandbox.uni.lu/minerva/",
            servlet: "galaxy.xhtml",
            model: 'pdmap_feb17_uncached',
            invalid: true
        };

        $scope.intersection = {
            genes: []
        };

        $scope.messages = {
            error: "",
            loading: "",
            totalRequests: 0,
            finishedRequests: 0,
            numTMIDs: {subset1: 0, subset2: 0},
            numVIDs: {subset1: 0, subset2: 0}
        };

        $scope.$watch('variantDB', function(vdb, vdbOld) {
            if (vdb.selectedGenes !== vdbOld.selectedGenes || vdb.regions !== vdbOld.regions || vdb.server !== vdbOld.server) {
                cleanup();
            }
            $scope.variantDB.invalid = !vdb.server || (!!vdb.regions && !!vdb.selectedGenes);
        }, true);

        $scope.downloadFile = function() {
            var blob =  new Blob([$scope.variantDB.file], {type: "text/plain;charset=utf-8"});
            saveAs(blob, "variantSummary.tsv");
        };

        var checkPDMapSanity = function() {
            $scope.pdmap.invalid = !$scope.pdmap.user ||
                !$scope.pdmap.password ||
                !$scope.pdmap.server ||
                !$scope.variantDB.data.length ||
                $scope.messages.finishedRequests !== $scope.messages.totalRequests ||
                $scope.variantDB.running;
        };
        $scope.$watch('pdmap', checkPDMapSanity, true);

        var getTMIDs = function() {
            var dfd = $.Deferred();
            runAllQueries(function() {
                $.ajax({
                    url: pageInfo.basePath + '/chart/clearGrid',
                    method: 'POST',
                    data: {
                        charttype: 'cleargrid',
                    }
                }).then(function() {
                    $.ajax({
                        url: pageInfo.basePath + '/chart/analysisGrid',
                        type: 'POST',
                        data: {
                            concept_key: '',
                            result_instance_id1: GLOBAL.CurrentSubsetIDs[1],
                            result_instance_id2: GLOBAL.CurrentSubsetIDs[2]
                        }
                    }).then(function(res) {
                        var ids = [];
                        JSON.parse(res).rows.map(function(d) {
                            ids.push({id: d.patient, subset: d.subset === 'subset1' ? 1 : 2});
                        });
                        dfd.resolve(ids);
                    });

                });
            });
            return dfd.promise();
        };

        var setIntersectionGenes = function() {
            $.ajax({
                url: pageInfo.basePath + '/SmartR/variantDB',
                type: 'POST',
                async: false,
                data: {
                    server: $scope.variantDB.server,
                    path: '/validgenes/',
                    filter_command: ''
                },
                success: function(res) {
                    var variantDBGeneList = JSON.parse(res);
                    variantDBGeneList = variantDBGeneList.map(function(d) { return d.toLowerCase(); });

                    var PDMAP_GENES = ['AARS2','ABCB1','ABCB7','ABHD5','ABL1','ACACA','ACACB','ACAD9','ACADL','ACADM','ACADS','ACADVL','ACAT1','ACLY','ACMSD','ACO1','ACO2','ACSL1','ACSL3','ACSL4','ACSL5','ACSL6','ACSS1','ACSS2','ACTN2','ACTR1A','ACTR2','ACTR3','ADCY1','ADCY2','ADCY3','ADCY4','ADCY5','ADCY6','ADCY7','ADCY8','ADCY9','ADCYAP1','ADD1','ADH1A','ADHFE1','ADORA2A','ADRM1','AGER','AGO2','AGPAT1','AGPAT2','AGPAT3','AGPAT4','AHCYL1','AIMP2','AKAP1','AKAP5','AKAP9','AKR1A1','AKR1B1','AKR1C3','AKT1','AKT1S1','AKT2','AKT3','ALDH1A1','ALDH2','ALDH3A1','ALDH9A1','ALDOA','AMBRA1','AMFR','ANPEP','ANXA2','APAF1','APC','APP','ARF5','ARG1','ARHGEF7','ARRB2','ARSG','ATAD1','ATAT1','ATF3','ATF4','ATF6','ATF6B','ATG10','ATG101','ATG12','ATG13','ATG14','ATG16L1','ATG3','ATG4B','ATG5','ATG7','ATG9B','ATP13A2','ATP1A1','ATP1A3','ATP1B1','ATP2A2','ATP2B1','ATP2B2','ATP2B3','ATP2B4','ATP5A1','ATP5B','ATP5C1','ATP5D','ATP5E','ATP5EP2','ATP5F1','ATP5G1','ATP5G2','ATP5G3','ATP5H','ATP5I','ATP5J','ATP5J2','ATP5L','ATP5L2','ATP5O','ATP6V0A1','ATP6V0A2','ATP6V0B','ATP6V0C','ATP6V0D1','ATP6V0D2','ATP6V0E1','ATP6V0E2','ATP6V1A','ATP6V1B1','ATP6V1B2','ATP6V1C1','ATP6V1C2','ATP6V1D','ATP6V1E1','ATP6V1E2','ATP6V1G1','ATP6V1G2','ATP6V1H','ATPAF2','ATPIF1','ATXN3','BACE1','BAD','BAG1','BAG2','BAG3','BAG4','BAG5','BAG6','BAK1','BAX','BBC3','BBIP1','BCAP31','BCL2','BCL2L1','BCL2L11','BCS1L','BDH1','BDNF','BECN1','BFAR','BID','BLVRA','BMF','BNIP3','BNIP3L','BOLA3','BPGM','BRSK2','BSN','BST1','BTRC','C12orf65','C1QA','C3','CABP1','CACNA1C','CACNA1D','CACNA2D1','CACNA2D2','CACNA2D3','CACNA2D4','CACNB1','CACNB2','CACNB3','CACNG2','CACNG3','CACNG4','CACNG8','CADPS2','CALM1','CALR','CAMK2A','CAMK2B','CAMK2D','CAMK2G','CAMK4','CAMKK2','CAPN1','CAPN2','CAPNS1','CASP1','CASP2','CASP3','CASP4','CASP6','CASP7','CASP8','CASP9','CAT','CCDC62','CCL2','CCL20','CCL3','CCL4','CCNB1','CCNE1','CCNE2','CD14','CD200','CD200R1','CD22','CD36','CD47','CDC37','CDC42','CDH1','CDK1','CDK2','CDK5','CDK5R1','CDKN1A','CDKN1B','CDKN2A','CEBPB','CELSR3','CFL1','CFL2','CFLAR','CHCHD2','CHCHD4','CHMP2B','CHRM1','CHUK','CISD1','CKMT1A','CLCN7','CLN3','CLPB','CLPP','COASY','COMT','COPS5','COX10','COX14','COX15','COX4I1','COX4I2','COX5A','COX5B','COX6A1','COX6A2','COX6B1','COX6C','COX7A1','COX7A2','COX7A2L','COX7B','COX7C','COX8A','CP','CPLX1','CPLX2','CPT1A','CPT1B','CPT2','CRADD','CREB1','CREB3','CREB3L1','CREB3L2','CREB3L3','CREB3L4','CREBBP','CRTC1','CRTC2','CS','CSNK1A1','CSNK1D','CSNK1E','CSNK2A1','CSNK2A2','CSNK2B','CTNNB1','CTNND2','CTSA','CTSB','CTSD','CTSF','CTTN','CUL1','CUL3','CUL4A','CX3CL1','CX3CR1','CXCL1','CXCL2','CYBA','CYBB','CYCS','D2HGDH','DAPK1','DARS2','DAXX','DBNL','DBT','DCTN1','DDB1','DDC','DDIT3','DDIT4','DDRGK1','DECR1','DEPDC5','DEPTOR','DERL1','DGAT1','DGAT2','DGKQ','DGUOK','DIABLO','DKK1','DLAT','DLD','DLG1','DLG4','DNAJA1','DNAJB1','DNAJB2','DNAJB9','DNAJC13','DNAJC5','DNAJC6','DNM1','DNM1L','DNMT1','DPYSL2','DRD1','DRD2','DRD3','DYNC1H1','DYNLL1','DYNLL2','E2F1','ECHS1','ECI1','EDEM1','EDEM2','EDEM3','EEF2K','EIF2A','EIF2AK2','EIF2AK3','EIF2AK4','EIF2S1','EIF4E','EIF4EBP1','EIF4G1','ELOVL1','ELOVL2','ELOVL3','ELOVL4','ELOVL5','ELOVL6','ELOVL7','EMX2','EN1','ENO1','EP300','EPB41L1','ERC1','ERC2','ERCC6','ERCC8','ERLEC1','ERN1','ERN2','ERO1A','ERP29','ESRRA','EXOC1','EXOC2','EXOC3','EXOC4','EXOC5','EXOC6','EXOC7','EXOC8','EZR','FADD','FAF1','FAM47E','FAS','FASLG','FASN','FASTKD2','FBP1','FBXL20','FBXO7','FBXW7','FCGR1A','FCGR2A','FCGR2B','FCGR3A','FGF20','FGF8','FH','FIS1','FKBP4','FLOT1','FLOT2','FOXA1','FOXA2','FOXO1','FOXO3','FOXRED1','FTH1','FTL','FTMT','FUBP1','FXN','FYN','FZD1','FZD3','FZD6','G6PC','G6PD','GABARAP','GABARAPL1','GABARAPL2','GABPA','GAK','GALM','GAPDH','GARS','GAS2','GATA2','GBA','GCH1','GCHFR','GCK','GCLC','GCLM','GDNF','GFM1','GFRA1','GGT1','GHITM','GHRL','GIGYF2','GK','GLA','GLB1','GLRX','GLRX2','GLUD1','GNAI1','GNAI2','GNAI3','GNAL','GNAQ','GNAS','GPAM','GPAT2','GPAT3','GPAT4','GPD1','GPD2','GPI','GPNMB','GPR37','GPR37L1','GPX1','GPX4','GRIA1','GRIA2','GRIA3','GRIA4','GRIN1','GRIN2A','GRIN2B','GRIN2C','GRIN2D','GRIP1','GRK5','GRM1','GRM3','GRM4','GRM5','GSK3A','GSK3B','GSN','GSR','GSS','GSTP1','GZMB','HADH','HADHA','HADHB','HAMP','HAX1','HDAC6','HEPH','HERPUD1','HEXA','HEXB','HGS','HIF1A','HIP1R','HK1','HK2','HM13','HMGB1','HMGCL','HMGCS2','HMOX1','HNF4A','HNRNPF','HRH1','HSD17B12','HSDL1','HSF1','HSP90AA1','HSP90B1','HSPA1A','HSPA1B','HSPA1L','HSPA4','HSPA5','HSPA6','HSPA8','HSPA9','HSPB1','HSPB8','HSPD1','HSPE1','HTR2C','HTRA2','HTT','HYOU1','ICAM1','IDH2','IDH3A','IDH3B','IDH3G','IFNG','IFNGR1','IFNGR2','IGF1R','IGF2R','IKBKB','IKBKG','IL10','IL13','IL1B','IL1R1','IL4','IL6','INPP5F','IRAK4','IREB2','IRF1','IRS1','ITGAM','ITGB1','ITGB2','ITLN1','ITPR1','ITPR2','ITPR3','JAK1','JAK2','JUN','KARS','KAT2A','KCNIP3','KCNJ11','KCNJ6','KCNJ8','KCNN1','KCNN2','KCNN3','KEAP1','KIF1A','KIF5B','KLC1','L2HGDH','LAG3','LAMB1','LAMP1','LAMP2','LAMP3','LAMTOR1','LAMTOR2','LAMTOR3','LCLAT1','LDHA','LDHB','LGALS3','LIMK1','LIMK2','LINGO1','LMNA','LMX1A','LMX1B','LONP1','LPIN1','LPIN2','LPIN3','LRP5','LRP6','LRPPRC','LRRK2','LTF','MAF','MAFF','MAG','MAOA','MAOB','MAP1B','MAP1LC3A','MAP1LC3B','MAP2','MAP2K3','MAP2K4','MAP2K6','MAP2K7','MAP3K5','MAP3K7','MAPK1','MAPK10','MAPK12','MAPK13','MAPK14','MAPK7','MAPK8','MAPK8IP3','MAPK9','MAPKAP1','MAPRE1','MAPT','MARCH5','MARCH6','MARK2','MBOAT1','MBOAT2','MBTPS1','MBTPS2','MCCC1','MCEE','MCL1','MCOLN1','MCU','MDH2','MDM2','MEF2C','MEF2D','MERTK','MFF','MFN1','MFN2','MICU1','MIR128-1','MIR128-2','MIR133B','MIR155','MIR184','MIR29B1','MIR29B2','MIR30A','MIR367','MIR9-1','MIR9-2','MIRLET7A1','MITF','MKL1','MLST8','MMAA','MMP1','MMP16','MMP3','MMP9','MOBP','MPC1','MPC2','MPO','MRPL3','MRPS16','MRPS22','MSN','MSR1','MT-ATP6','MT-ATP8','MT-CO1','MT-CO2','MT-CO3','MT-CYB','MT-ND1','MT-ND2','MT-ND3','MT-ND4','MT-ND4L','MT-ND5','MT-ND6','MT-RNR1','MT-TC','MT-TE','MT-TF','MT-TH','MT-TI','MT-TK','MT-TL1','MT-TL2','MT-TN','MT-TP','MT-TQ','MT-TR','MT-TS1','MT-TS2','MT-TT','MT-TV','MT-TW','MT3','MTFMT','MTOR','MTPAP','MUL1','MYD88','MYO6','N/A','NAF1','NAGLU','NAMPT','NAPA','NAPB','NBR1','NCF1','NCF2','NCF4','NCK1','NCK2','NCOR2','NCS1','NDFIP1','NDN','NDRG1','NDUFA1','NDUFA10','NDUFA11','NDUFA12','NDUFA13','NDUFA2','NDUFA3','NDUFA4','NDUFA5','NDUFA6','NDUFA7','NDUFA8','NDUFA9','NDUFAB1','NDUFAF1','NDUFAF2','NDUFAF3','NDUFAF4','NDUFAF5','NDUFAF6','NDUFB1','NDUFB10','NDUFB11','NDUFB2','NDUFB3','NDUFB4','NDUFB5','NDUFB6','NDUFB7','NDUFB8','NDUFB9','NDUFC1','NDUFC2','NDUFS1','NDUFS2','NDUFS3','NDUFS4','NDUFS5','NDUFS6','NDUFS7','NDUFS8','NDUFV1','NDUFV2','NDUFV3','NEDD4','NEDD4L','NEDD8','NEFL','NEK1','NEU1','NFATC1','NFATC2','NFATC3','NFATC4','NFE2L2','NFKB1','NFKBIA','NFU1','NFYA','NFYB','NFYC','NGF','NGFR','NKX6-1','NLRP3','NMD3','NME2','NMRK1','NMT1','NNT','NONO','NOS2','NOS3','NOTCH1','NPC1','NPEPPS','NPLOC4','NPRL2','NPRL3','NQO1','NR1H4','NR3C1','NR4A2','NRF1','NRON','NRP1','NSF','NSMF','NTF3','NTRK1','NTRK2','NUB1','NUBPL','NUCKS1','OGDH','OMA1','OMG','OPA1','OPRD1','OPTN','OS9','OTX2','OXCT1','P2RX7','P2RY6','PACRG','PAK6','PANK2','PARK2','PARK7','PARL','PARP1','PARP16','PBX1','PC','PCCA','PCCB','PCLO','PDCD6IP','PDHA1','PDHB','PDHX','PDIA2','PDIA6','PDK1','PDK2','PDK3','PDK4','PDP1','PDP2','PDPK1','PDPR','PFKFB3','PFKL','PFKM','PFN1','PFN2','PGAM4','PGAM5','PGD','PGK1','PGLS','PGLYRP4','PGM1','PHB','PHB2','PHLPP1','PIAS2','PIAS3','PIAS4','PICK1','PIDD1','PIK3C3','PIK3R1','PIK3R2','PIK3R4','PIN1','PINK1','PITX3','PKLR','PLA2G6','PLD2','PLEC','PLK2','PLK3','PLXNA1','PM20D1','PMAIP1','PMPCA','PMPCB','POLG','POLG2','POLRMT','PPARA','PPARG','PPARGC1A','PPFIA1','PPFIA2','PPFIA3','PPFIA4','PPIF','PPP1CA','PPP1CB','PPP1CC','PPP1R15A','PPP1R15B','PPP2CA','PPP2CB','PPP3CA','PPP3CB','PPP3CC','PPP3R1','PPP3R2','PPP5C','PRDX1','PRDX2','PRDX3','PRDX4','PRDX5','PRDX6','PRKAA1','PRKAA2','PRKAB1','PRKAB2','PRKACA','PRKACB','PRKACG','PRKAG1','PRKAG2','PRKAG3','PRKAR2B','PRKCD','PRKCI','PRKD1','PRODH','PROX1','PRR5','PSAP','PSEN1','PSEN2','PSMA1','PSMA2','PSMA3','PSMA4','PSMA5','PSMA6','PSMA7','PSMB1','PSMB2','PSMB3','PSMB4','PSMB5','PSMB6','PSMB7','PSMC1','PSMC2','PSMC3','PSMC4','PSMC5','PSMC6','PSMD1','PSMD11','PSMD12','PSMD14','PSMD2','PSMD3','PSMD4','PSMD6','PSMD7','PSMD8','PSMD9','PTBP1','PTEN','PTGDS','PTGES','PTGES2','PTGES3','PTGS1','PTGS2','PTK2','PTMA','PTPN1','PTPRC','PTRH2','PTS','PUM1','PYCR1','RAB10','RAB11A','RAB27A','RAB29','RAB32','RAB3A','RAB3GAP1','RAB5A','RAB5B','RAB7A','RAB7B','RAB8A','RAC1','RAC2','RACK1','RAD23A','RAD23B','RALB','RANBP9','RAP1GAP','RAP1GAP2','RARS2','RASGRP2','RB1CC1','RDX','RELA','RET','RGS4','RHBDD1','RHEB','RHO','RHOA','RHOT1','RHOT2','RICTOR','RIMBP2','RIMS1','RIMS2','RIMS3','RIMS4','RIPK1','RIT2','RNASE1','RNF139','RNF185','RNF41','RNF5','ROCK1','ROCK2','RPH3A','RPS15','RPS6KA1','RPS6KA2','RPS6KA3','RPS6KA6','RPS6KB1','RPTOR','RRAGA','RRAGB','RRAGC','RRAGD','RRM2B','RTN4','RTN4R','RUBCN','RXRA','RYK','RYR1','RYR2','RYR3','S100B','SCAMP1','SCARA5','SCARB2','SCFD1','SCO1','SCO2','SDHA','SDHAF1','SDHB','SDHC','SDHD','SEC16A','SEL1L','SEMA3A','SENP2','SENP5','SEPT5','SESN2','SESN3','SETD1A','SFN','SFPQ','SGTA','SHANK2','SIAH3','SIGMAR1','SIK2','SIPA1L2','SIRPA','SIRT1','SIRT2','SIRT3','SIRT5','SKP1','SKP2','SLC11A2','SLC16A1','SLC16A4','SLC16A7','SLC18A2','SLC1A1','SLC1A2','SLC1A3','SLC25A1','SLC25A10','SLC25A11','SLC25A12','SLC25A14','SLC25A20','SLC25A23','SLC25A27','SLC25A28','SLC25A37','SLC25A4','SLC25A5','SLC25A6','SLC2A1','SLC36A1','SLC38A9','SLC3A2','SLC40A1','SLC41A1','SLC45A3','SLC6A2','SLC6A3','SLC7A11','SLC7A5','SLC8A1','SLC8A2','SLC8A3','SLC8B1','SMURF1','SNAP25','SNCA','SNCAIP','SNX1','SNX12','SNX2','SNX3','SNX32','SNX5','SNX6','SOCS1','SOCS3','SOD1','SOD2','SOX6','SPR','SQSTM1','SREBF1','SRF','SSBP1','SSH1','SSH3','ST13','STBD1','STEAP1','STEAP2','STEAP3','STK11','STK24','STK25','STK3','STK39','STUB1','STX1A','STX1B','STX6','STXBP1','STXBP5','STXBP6','SUCLA2','SUCLG1','SUCLG2','SUMO1','SURF1','SV2A','SV2B','SV2C','SVIP','SYK','SYN1','SYN2','SYN3','SYNJ1','SYP','SYT1','SYT11','SYT12','SYT4','SYVN1','TAB2','TAB3','TACO1','TAF1','TAOK3','TAT','TBC1D24','TBC1D5','TBL2','TCIRG1','TECR','TF','TFAM','TFB1M','TFB2M','TFDP1','TFE3','TFEB','TFEC','TFR2','TFRC','TGFB1','TH','TIAM1','TIMM17A','TIMM17B','TIMM23','TIMM44','TIMM50','TK2','TLR2','TLR3','TLR4','TMBIM6','TMEM129','TMEM70','TNF','TNFRSF10A','TNFRSF1A','TOMM20','TOMM22','TOMM40','TOMM5','TOMM6','TOMM7','TOMM70','TP53','TPCN2','TPI1','TPP1','TPPP','TRADD','TRAF2','TRAF6','TRAK1','TRAP1','TREM2','TRIB3','TRIM17','TRIM25','TRIM32','TRPC1','TRPC6','TSC1','TSC2','TSFM','TSG101','TSPOAP1','TUBB','TUBB4A','TUBB6','TUFM','TWNK','TXN','TXNIP','TXNRD1','TXNRD2','TYR','TYROBP','UBA1','UBA6','UBB','UBE2J1','UBE2L3','UBE2N','UBE2V1','UBE3A','UBQLN1','UBQLN2','UBQLN4','UBXN1','UCHL1','UCP2','UCP3','ULK1','UNC13A','UNC13B','UNC13C','UQCR10','UQCR11','UQCRB','UQCRC1','UQCRC2','UQCRFS1','UQCRFS1P1','UQCRH','UQCRQ','USP13','USP14','USP25','UVRAG','VAMP2','VAMP8','VCAM1','VCP','VDAC1','VDAC2','VDAC3','VHL','VIM','VIMP','VIP','VMP1','VPS11','VPS13C','VPS18','VPS26A','VPS26B','VPS29','VPS35','VTI1A','VTI1B','WAS','WASF1','WASF3','WASL','WDR45','WFS1','WIPI2','WISP1','WNT1','WNT2','WNT3','WNT3A','WNT5A','XBP1','XIAP','XPO1','XRCC6','YME1L1','YOD1','YWHAB','YWHAE','YWHAG','YWHAQ','YWHAZ','YY1','ZFYVE1','ZKSCAN3','ZNF746','ZSCAN21'].map(function(d) { return d.toLowerCase(); });

                    var intersection = smartRUtils._intersectArrays(variantDBGeneList, PDMAP_GENES);
                    $scope.intersection.genes = intersection;
                }
            });
        };
        setIntersectionGenes();

        var getVariantDBIDs = function(tmIDs) {
            var dfd = $.Deferred();
            $.ajax({
                url: pageInfo.basePath + '/SmartR/variantDB',
                type: 'POST',
                data: {
                    server: $scope.variantDB.server,
                    path: '/individuals/POST/',
                    filter_command: 'getfields!eq!id,comments&comments!eqa!' + tmIDs.map(function(d) { return d.id; }).map(function(d) { return d.toLowerCase(); }).join(','),
                },
                success: function(res) {
                    var data = JSON.parse(res);
                    var vIDIdx = data.fields.indexOf('id');
                    var tmIDIdx = data.fields.indexOf('comments');
                    var ids = [];
                    data.values.forEach(function(d) {
                        var hits = tmIDs.filter(function(e) { return e.id === d[tmIDIdx]; });
                        hits.forEach(function(hit) {
                            ids.push({
                                vID: d[vIDIdx],
                                subset: hit.subset
                            });
                        });
                    });
                    if (ids.length) {
                        dfd.resolve(ids);
                    } else {
                        dfd.reject('No matching Subject IDs found in VariantDB.');
                    }
                },
                failure: function() { dfd.reject('An error occured when trying to communicate with VariantDB.'); }
            });
            return dfd.promise();
        };

        var getFilterString = function() {
            var filters1 = [];
            var filters2 = [];
            var filtersString = '';
            for (var key1 in $scope.variantDB.func_refgene) {
                if ($scope.variantDB.func_refgene.hasOwnProperty(key1) && $scope.variantDB.func_refgene[key1]) {
                    filters1.push(key1);
                }
            }
            for (var key2 in $scope.variantDB.exonicfunc_refgene) {
                if ($scope.variantDB.exonicfunc_refgene.hasOwnProperty(key2) && $scope.variantDB.exonicfunc_refgene[key2]) {
                    filters1.push(key2);
                }
            }

            filtersString += filters1.length ? '&func_refgene!ov!' + filters1.join(',') : filtersString;
            filtersString += filters2.length ? '&exonicfunc_refgene!ov!' + filters2.join(',') : filtersString;
            filtersString += '&field_1000g2015aug_eur,exac_all,esp6500si_ea!lt!' + $scope.variantDB.misc.globalMAF;
            return filtersString;
        };

        var getVariantDBRequestsForGenes = function(variantDBIDs) {
            var ids = variantDBIDs.map(function(d) { return d.vID; });
            var dfd = $.Deferred();
            var genes = [];
            if ($scope.variantDB.selectedGenes.length) {
                genes = $scope.variantDB.selectedGenes.split(',').map(function(d) { return d.trim().toLowerCase(); })
                    .filter(function(d) { return $scope.intersection.genes.indexOf(d) !== -1; });
                if (! genes.length) {
                    dfd.reject("None of the specified genes could be found in VariantDB.");
                }
            } else {
                genes = $scope.intersection.genes;
            }

            $.ajax({
                url: pageInfo.basePath + '/SmartR/variantDB',
                type: 'POST',
                data: {
                    server: $scope.variantDB.server,
                    path: '/variant_all/POST/',
                    filter_command: 'splitcommand!eq!t&getfields!eq!gene_at_position,start,reference,alleleseq,variant_genotypes&shown_individuals!eq!' + ids.join(',') +
                        '&c01,c11!ov!' + ids.join(',') + '&gene!eq!' + genes.join(',') + getFilterString()
                },
                success: function(res) { dfd.resolve(JSON.parse(res)); },
                failure: function() { dfd.reject('An error occured when trying to communicate with VariantDB.'); }

            });
            return dfd.promise();
        };

        var getVariantDBRequestsForRegions = function(ids) {
            var dfd = $.Deferred();
            var regions = $scope.variantDB.regions;
            regions = regions.split(',').map(function(d) { return d.trim(); });
            var chrs = regions.map(function(d) { return d.split(':')[0]; });
            var starts = regions.map(function(d) { return d.split(':')[1].split('-')[0]; });
            var stops = regions.map(function(d) { return d.split(':')[1].split('-')[1]; });
            $.ajax({
                url: pageInfo.basePath + '/SmartR/variantDB',
                type: 'POST',
                data: {
                    server: $scope.variantDB.server,
                    path: '/variant_all/POST/',
                    filter_command: 'splitcommand!eq!t&getfields!eq!start,reference,alleleseq,variant_genotypes&shown_individuals!eq!' +
                            ids.join(',') + '&c01,c11!ov!' + ids.join(',') + '&chrom!eq!' + chrs.join(',') + '&start!gt!' +
                            starts.join(',') + '&start!lt!' + stops.join(',') + getFilterString()
                },
                success: function(res) { dfd.resolve(res); },
                failure: function() { dfd.reject('An error occured when trying to communicate with VariantDB.'); }
            });
            return dfd.promise();
        };

        var getVariantDBData = function(request) {
            var dfd = $.Deferred();
            $.ajax({
                url: pageInfo.basePath + '/SmartR/variantDB',
                type: 'POST',
                data: {
                    server: $scope.variantDB.server,
                    path: '/variant_all/POST/',
                    filter_command: request,
                },
                success: function(res) {
                    dfd.resolve(res);
                },
                failure: function() {
                    dfd.reject('An error occured when trying to communicate with VariantDB.');
                }
            });
            return dfd.promise();
        };

        var _prepareData = function(data, subset, ids) {
            var variantData = JSON.parse(data);
            var indices = {};
            indices['pos'] = variantData.fields.indexOf('start');
            indices['ref'] = variantData.fields.indexOf('reference');
            indices['alt'] = variantData.fields.indexOf('alleleseq');
            indices['chr'] = variantData.fields.indexOf('chrom');
            indices['gene'] = variantData.fields.indexOf('gene_at_position');
            indices['ids'] = [];
            ids.forEach(function(variantDBID) {
                var idx = variantData.fields.indexOf(variantDBID);
                if (idx !== -1) {
                    indices['ids'].push(idx);
                }
            });

            var file = '';
            variantData.values.forEach(function(d) {
                var pos = d[indices['pos']];
                var ref = d[indices['ref']];
                var alt = d[indices['alt']];
                var chr = d[indices['chr']];
                var gene = d[indices['gene']];
                var variants = indices['ids'].map(function(idIndex) {
                    return d[idIndex];
                });
                var frq = variants.filter(function(d,i) {
                    file += ids[i] + '\t' + gene + '\t' + pos + '\t' + subset + '\t' + d + '\n';
                    return d.indexOf('1') !== -1;
                }).length / ids.length;
                if (! isNaN(frq) && frq !== 0 && frq >= $scope.variantDB.misc.cohortMAF) {
                    $scope.variantDB.data.push({
                        pos: pos,
                        ref: ref,
                        alt: alt,
                        chr: chr,
                        frq: frq,
                        gene: gene,
                        subset: subset
                    });
                }
            });
            $scope.variantDB.file = file;
        };

        var prepareData = function(data, variantDBIDs) {
            var subsets = smartRUtils.unique(variantDBIDs.map(function(d) { return d.subset; }));
            subsets.forEach(function(subset) {
                var ids = variantDBIDs.filter(function(d) { return d.subset === subset; })
                    .map(function(d) { return d.vID; });
                _prepareData(data, subset, ids);
            });
            checkPDMapSanity();
            $scope.$apply();
        };

        var handleError = function(err) {
            $scope.variantDB.running = false;
            $scope.messages.finishedRequests += 1;
            $scope.messages.error = err;
            $scope.$apply();
        };

        var handleRequests = function(requests, variantDBIDs) {
            if (! requests.length && $scope.messages.finishedRequests === $scope.messages.totalRequests) {
                $scope.variantDB.running = false;
                $scope.variantDB.showViz = true;
                checkPDMapSanity();
                $scope.$apply();
                return;
            }
            if (! $scope.variantDB.running) { // if error
                return;
            }
            $.when(getVariantDBData(requests[0])).then(function(data) {
                prepareData(data, variantDBIDs);
                $scope.messages.finishedRequests += 1;
                $scope.$apply();
                handleRequests(requests.slice(1), variantDBIDs);
            }, handleError);
        };

        var cleanup = function() {
            $scope.variantDB.file = 'subject\tgene\tpos\tsubset\tvariant\n';
            $scope.variantDB.data = [];
            $scope.variantDB.showViz = false;
            $scope.messages.error = '';
            $scope.messages.totalRequests = 0;
            $scope.messages.finishedRequests = 0;
            $scope.messages.loading = '';
        };

        $scope.fetchVariantDB = function() {
            cleanup();
            $scope.variantDB.running = true;
            $scope.variantDB.showViz = false;
            getTMIDs().then(function(tmIDs) {
                $scope.messages.numTMIDs.subset1 = tmIDs.filter(function(d) { return d.subset === 1; }).length;
                $scope.messages.numTMIDs.subset2 = tmIDs.filter(function(d) { return d.subset === 2; }).length;
                $scope.variantDB.running = true;
                getVariantDBIDs(tmIDs).then(function(variantDBIDs) {
                    $scope.messages.numVIDs.subset1 = variantDBIDs.filter(function(d) { return d.subset === 1; }).length;
                    $scope.messages.numVIDs.subset2 = variantDBIDs.filter(function(d) { return d.subset === 2; }).length;
                    if ($scope.variantDB.regions) {
                        getVariantDBRequestsForRegions(variantDBIDs).then(function(requests) {
                            $scope.messages.totalRequests += requests.length;
                            $scope.$apply();
                            handleRequests(requests, variantDBIDs);
                        });
                    } else {
                        getVariantDBRequestsForGenes(variantDBIDs).then(function(requests) {
                            $scope.messages.totalRequests += requests.length;
                            $scope.$apply();
                            handleRequests(requests, variantDBIDs);
                        }, handleError);
                    }
                }, handleError);
            });
        };

        var _createPDMapLayout = function(subset, identifier) {
            var expression_value = '#TYPE=GENETIC_VARIANT\n';
            expression_value += '#GENOME_TYPE=UCSC\n';
            expression_value += '#GENOME_VERSION=hg38\n';
            expression_value += 'position\toriginal_dna\talternative_dna\tname\tcontig\tallel_frequency\n';

            $scope.variantDB.data.forEach(function(d) {
                if (d.subset === subset) {
                    expression_value += d.pos + '\t' + d.ref + '\t' + d.alt + '\t' + d.gene + '\t' + d.chr + '\t' + d.frq + '\n';
                }
            });

            return $.ajax({
                url: pageInfo.basePath + '/SmartR/pdMap',
                type: 'POST',
                data: {
                    url: $scope.pdmap.server + $scope.pdmap.servlet,
                    identifier: identifier,
                    login: $scope.pdmap.user,
                    password: $scope.pdmap.password,
                    model: $scope.pdmap.model,
                    expression_value: expression_value,
                }
            });
        };


        $scope.createPDMapLayout = function() {
            var cohorts = smartRUtils.countCohorts();
            var identifier1 = + new Date();
            var identifier2 = + new Date() + 1;
            _createPDMapLayout(1, identifier1).then(function() {
                if (cohorts === 2) {
                    _createPDMapLayout(2, identifier2).then(function() {
                        window.open($scope.pdmap.server);
                    });
                } else {
                    window.open($scope.pdmap.server);
                }
            });
        };

    }]);

