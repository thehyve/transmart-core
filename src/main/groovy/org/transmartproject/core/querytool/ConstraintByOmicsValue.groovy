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

    static enum Operator {

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

    static enum OmicsType {
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
                    { throw new IllegalArgumentException("No OmicsType for value $value") }
        }
    }

    // ProjectionType names should correspond to an existing column in the table holding the omics data
    static enum ProjectionType {
        // Gene expression types
        RAW_INTENSITY ("Raw Intensity"),
        LOG_INTENSITY ("Log Intensity"),
        ZSCORE ("Z-Score"),

        // RNASEQ types (Zscore already specified)
        NORMALIZED_READCOUNT ("Normalized Readcount"),
        LOG_NORMALIZED_READCOUNT ("Log Normalized Readcount"),

        // Proteomics has 'intensity'
        INTENSITY ("Raw Intensity"),

        // ACGH and VCF will have a complex selector defining the entire filter
        COMPLEX ("Complex")

        final String value

        protected ProjectionType(String value) {
            this.value = value
        }

        static ProjectionType forValue(String value) {
            values().find { value == it.value } ?:
                    { throw new IllegalArgumentException("No ProjectionType for value $value")}
        }
    }

    // Different classes of omics filters
    static enum FilterType {
        SINGLE_NUMERIC,
        ACGH,
        VCF
    }

    // centralized point to get metadata on the marker type
    // allows to parameterize the dialog and queries for the simpler omics data types
    static def markerInfo = [
         "Gene Expression": [data_table: "deapp.de_subject_microarray_data",
                            annotation_table: "deapp.de_mrna_annotation",
                            id_column: "probeset_id",
                            annotation_id_column: "probeset_id",
                            selector_column: "gene_symbol",
                            allowed_projections: [ProjectionType.RAW_INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE],
                            filter_type: FilterType.SINGLE_NUMERIC],
        "RNASEQ_RCNT": [data_table: "deapp.de_subject_rnaseq_data",
                        annotation_table: "deapp.de_chromosomal_region",
                        id_column: "region_id",
                        annotation_id_column: "region_id",
                        selector_column: "gene_symbol",
                        allowed_projections: [ProjectionType.NORMALIZED_READCOUNT, ProjectionType.LOG_NORMALIZED_READCOUNT, ProjectionType.ZSCORE],
                        filter_type: FilterType.SINGLE_NUMERIC],
        "PROTEOMICS":  [data_table: "deapp.de_subject_protein_data",
                        annotation_table: "deapp.de_protein_annotation",
                        id_column: "protein_annotation_id",
                        annotation_id_column: "id",
                        selector_column: "uniprot_name",
                        allowed_projections: [ProjectionType.INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE],
                        filter_type: FilterType.SINGLE_NUMERIC],
        "Chromosomal": [data_table: "",
                        annotation_table: "",
                        id_column: "",
                        annotation_id_column: "",
                        selector_column: "",
                        allowed_projections: [ProjectionType.COMPLEX],
                        filter_type: FilterType.ACGH],
        "MIRNA_QPCR":  [data_table: "deapp.de_subject_mirna_data",
                        annotation_table: "deapp.de_qpcr_mirna_annotation",
                        id_column: "probeset_id",
                        annotation_id_column: "probeset_id",
                        selector_column: "mirna_id",
                        allowed_projections: [ProjectionType.RAW_INTENSITY, ProjectionType.LOG_INTENSITY, ProjectionType.ZSCORE],
                        filter_type: FilterType.SINGLE_NUMERIC],
        "VCF":         [data_table: "",
                        annotation_table: "",
                        id_column: "",
                        annotation_id_column: "",
                        selector_column: "",
                        allowed_projections: [ProjectionType.COMPLEX],
                        filter_type: FilterType.VCF]
            ]
}
