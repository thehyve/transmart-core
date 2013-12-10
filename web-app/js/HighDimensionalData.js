
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
            "MRNA_AFFYMETRIX"   : {"platform" : "MRNA_AFFYMETRIX", "type" : "Gene Expression"},
            "MIRNA_AFFYMETRIX"  : {"platform" : "MIRNA_AFFYMETRIX", "type" : "QPCR MIRNA"},
            "MIRNA_SEQ"         : {"platform" : "MIRNA_SEQ", "type" : "SEQ MIRNA"},
            "RBM"               : {"platform" : "RBM", "type" : "RBM"},
            "PROTEIN"           : {"platform" : "PROTEIN", "type" : "PROTEOMICS"},
            "SNP"               : {"platform" : "SNP", "type" : "SNP"},
            "RNASEQ"            : {"platform" : "RNASEQ", "type" : "RNASEQ"}
        };
    }

    this.supportedTypes = getSupportedTypes();
}

