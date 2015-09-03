package org.transmartproject.core.querytool

import groovy.transform.Immutable

@Immutable
class ConstraintByOmicsValue {

    /**
     * The operator that will be used to compare the value with the constraint.
     */
    Operator operator

    /**
     * The second operand in the constraint.
     */
    String constraint

    /**
     * The omics data type
     */
    OmicsType omicsType

    /**
     * String to represent what part of omics data to check. This can hold e.g. a gene symbol
     */
    String selector

    /**
     * The projection
     */
    ProjectionType projectionType

    enum Operator {

        LOWER_THAN          ('LT'),
        LOWER_OR_EQUAL_TO   ('LE'),
        EQUAL_TO            ('EQ'),
        BETWEEN             ('BETWEEN'),
        GREATER_THAN        ('GT'),
        GREATER_OR_EQUAL_TO ('GE');

        final String value

        protected Operator(String value) {
            this.value = value
        }

        static Operator forValue(String value) {
            values().find { value == it.value } ?:
                { throw new IllegalArgumentException("No operator for value $value") }()
        }
    }

    public static final enum OmicsType {
        GENE_EXPRESSION ('Gene Expression'),
        RNASEQ_RCNT ('RNASEQ_RCNT'),
        PROTEOMICS ('PROTEOMICS'),
        CHROMOSOMAL ('Chromosomal'),
        MIRNA_QPCR ('MIRNA_QPCR'),
        VCF ('VCF')

        final String value

        protected OmicsType(String value) {
            this.value = value
        }

        static OmicsType forValue(String value) {
            values().find { value == it.value } ?:
                    { throw new IllegalArgumentException("No operator for value $value") }()
        }
    }

    // ProjectionType names should correspond to an existing column in the table holding the omics data
    public static final enum ProjectionType {
        // Gene expression types
        RAW_INTENSITY,
        LOG_INTENSITY,
        ZSCORE,

        // RNASEQ types (Zscore already specified)
        NORMALIZED_READCOUNT,
        LOG_NORMALIZED_READCOUNT,

        // Proteomics has 'intensity'
        INTENSITY
    }

    public static final ALLOWED_PROJECTIONS = [
            (OmicsType.GENE_EXPRESSION) : [ProjectionType.RAW_INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE],
            (OmicsType.RNASEQ_RCNT)     : [ProjectionType.NORMALIZED_READCOUNT, ProjectionType.LOG_NORMALIZED_READCOUNT, ProjectionType.ZSCORE],
            (OmicsType.PROTEOMICS)      : [ProjectionType.INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE],
            (OmicsType.MIRNA_QPCR)      : [ProjectionType.RAW_INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE]
    ]

    public static def getMarkerInfo(String markerType) {
        if (markerType.equals('Gene Expression')) {
            return [data_table: "deapp.de_subject_microarray_data",
                    annotation_table: "deapp.de_mrna_annotation",
                    id_column: "probeset_id",
                    annotation_id_column: "probeset_id",
                    selector_column: "gene_symbol",
                    allowed_projections: ["raw_intensity", "log_intensity", "zscore"]]
        }
        else if (markerType.equals("RNASEQ_RCNT")) {
            return [data_table: "deapp.de_subject_rnaseq_data",
                    annotation_table: "deapp.de_chromosomal_region",
                    id_column: "region_id",
                    annotation_id_column: "region_id",
                    selector_column: "gene_symbol",
                    allowed_projections: ["normalized_readcount", "log_normalized_readcount", "zscore"]]
        }
        else if (markerType.equals("PROTEOMICS")) {
            return [data_table: "deapp.de_subject_protein_data",
                    annotation_table: "deapp.de_protein_annotation",
                    id_column: "protein_annotation_id",
                    annotation_id_column: "id",
                    selector_column: "uniprot_name",
                    allowed_projections: ["intensity", "log_intensity", "zscore"]]
        }
        else if (markerType.equals("Chromosomal")) {
            return [data_table: "deapp.de_subject_protein_data",
                    annotation_table: "deapp.de_chromosomal_region",
                    id_column: "region_id",
                    annotation_id_column: "region_id",
                    selector_column: "gene_symbol",
                    allowed_projections: ["chip","segmented","flag","probloss","probnorm","probgain","probamp"]]
        }
        else if (markerType.equals("MIRNA_QPCR")) {
            return [data_table: "deapp.de_subject_mirna_data",
                    annotation_table: "deapp.de_qpcr_mirna_annotation",
                    id_column: "probeset_id",
                    annotation_id_column: "probeset_id",
                    selector_column: "mirna_id",
                    allowed_projections: ["raw_intensity", "log_intensity", "zscore"]]
        }

        return null
    }

}
