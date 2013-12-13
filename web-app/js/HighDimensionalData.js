
var HighDimensionalData = function () {

    /**
     * Get supported high dimensional data types
     * @returns {*}
     */
    var getSupportedTypes = function () {

        // check if tranSMART's global variable exists then use it
        if (typeof(HIGH_DIMENSIONAL_DATA) != "undefined") {
            return HIGH_DIMENSIONAL_DATA
        }

        return {
            "mrna"          : {"platform" : "MRNA_AFFYMETRIX", "type" : "Gene Expression"},
            "mirna_qpcr"    : {"platform" : "MIRNA_QPCR", "type" : "MIRNA_QPCR"},
            "mirna_seq"     : {"platform" : "MIRNA_SEQ", "type" : "MIRNA_SEQ"},
            "rbm"               : {"platform" : "RBM", "type" : "RBM"},
            "proteomics"        : {"platform" : "PROTEIN", "type" : "PROTEOMICS"},
            "snp"               : {"platform" : "SNP", "type" : "SNP"},
            "rnaseq"            : {"platform" : "RNA_AFFYMETRIX", "type" : "RNASEQ"}
        };
    }

    this.supportedTypes = getSupportedTypes();
}

