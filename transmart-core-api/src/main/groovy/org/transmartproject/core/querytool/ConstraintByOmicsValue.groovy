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
     * The highdimension data type
     */
    OmicsType omicsType

    /**
     * String to represent what part of highdimension data to check. This can hold e.g. a gene symbol
     */
    String selector

    /**
     * String to represent what property of the high dimensional data to check the selector against, e.g. geneSymbol
     */
    String property

    /**
     * The projection, should correspond to one of the static strings in {@link org.transmartproject.core.dataquery.highdim.projections.Projection}
     */
    String projectionType

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
        RNASEQ ('RNASEQ'),
        RNASEQ_RCNT ('RNASEQ_RCNT'),
        PROTEOMICS ('PROTEOMICS'),
        CHROMOSOMAL ('Chromosomal'),
        MIRNA_QPCR ('MIRNA_QPCR'),
        MIRNA_SEQ ('MIRNA_SEQ'),
        METABOLOMICS ('METABOLOMICS'),
        RBM ('RBM'),

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
}
