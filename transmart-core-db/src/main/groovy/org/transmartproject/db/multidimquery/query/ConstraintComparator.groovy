package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
class ConstraintComparator implements Comparator<Constraint> {

    static final List<Class<? extends Constraint>> constraintClassOrder = [
            TrueConstraint.class,
            Negation.class,
            Combination.class,
            AndConstraint.class,
            OrConstraint.class,
            SubSelectionConstraint.class,
            MultipleSubSelectionsConstraint.class,
            NullConstraint.class,
            BiomarkerConstraint.class,
            ModifierConstraint.class,
            FieldConstraint.class,
            ValueConstraint.class,
            RowValueConstraint.class,
            TimeConstraint.class,
            PatientSetConstraint.class,
            TemporalConstraint.class,
            ConceptConstraint.class,
            StudyNameConstraint.class,
            StudyObjectConstraint.class,
            RelationConstraint.class
    ]
    static final Map<Class<? extends Constraint>, Integer> constraintClassIndexes = constraintClassOrder.collectEntries {
        [(it): constraintClassOrder.indexOf(it)]
    }

    static final int indexForType(Class<? extends Constraint> type) {
        def index = constraintClassIndexes[type]
        if (index == null) {
            throw new QueryBuilderException("Constraint type not supported: ${type}.")
        }
        index.intValue()
    }

    @PackageScope
    static <T extends Comparable<T>> int compareValueList(List<T> l1, List<T> l2) {
        if (l1 == null && l2 == null) {
            return 0
        } else if (l1 == null) {
            return -1
        } else if (l2 == null) {
            return 1
        }
        if (l1.size() < l2.size()) {
            return -1
        } else if (l1.size() > l2.size()) {
            return 1
        }
        l1.sort()
        l2.sort()
        def i1 = l1.iterator()
        def i2 = l2.iterator()
        while (i1.hasNext()) {
            def c1 = i1.next()
            def c2 = i2.next()
            int res = c1 <=> c2
            if (res != 0) {
                return res
            }
        }
        return 0
    }

    @PackageScope
    static <T extends Comparable<T>> int compareNullable(T v1, T v2) {
        if (v1 == null && v2 == null) {
            return 0
        } else if (v1 == null) {
            return -1
        } else if (v2 == null) {
            return 1
        }
        return v1 <=> v2
    }

    int compareList(List<Constraint> l1, List<Constraint> l2) {
        if (l1 == null && l2 == null) {
            return 0
        } else if (l1 == null) {
            return -1
        } else if (l2 == null) {
            return 1
        }
        if (l1.size() < l2.size()) {
            return -1
        } else if (l1.size() > l2.size()) {
            return 1
        }
        def i1 = l1.iterator()
        def i2 = l2.iterator()
        while (i1.hasNext()) {
            def c1 = i1.next()
            def c2 = i2.next()
            int res = compare(c1, c2)
            if (res != 0) {
                return res
            }
        }
        return 0
    }

    int compareSameType(Constraint c1, Constraint c2) {
        switch (c1.class) {
            case TrueConstraint:
                return 0
            case Negation:
                return compare(((Negation) c1).arg, ((Negation) c2).arg)
            case Combination:
                def combination1 = (Combination) c1
                def combination2 = (Combination) c2
                def res = combination1.operator <=> combination2.operator
                if (res != 0) {
                    return res
                }
                return compareList(combination1.args, combination2.args)
            case AndConstraint:
                 OrConstraint:
                return compareList(((Combination) c1).args, ((Combination) c2).args)
            case SubSelectionConstraint:
                def res = ((SubSelectionConstraint) c1).dimension <=> ((SubSelectionConstraint) c2).dimension
                if (res != 0) {
                    return res
                }
                return compare(((SubSelectionConstraint) c1).constraint, ((SubSelectionConstraint) c2).constraint)
            case TemporalConstraint:
                def res = ((TemporalConstraint) c1).operator <=> ((TemporalConstraint) c2).operator
                if (res != 0) {
                    return res
                }
                return compare(((TemporalConstraint) c1).eventConstraint, ((TemporalConstraint) c2).eventConstraint)
            case RelationConstraint:
                def r1 = (RelationConstraint) c1
                def r2 = (RelationConstraint) c2
                def res = r1.relationTypeLabel <=> r2.relationTypeLabel ?: r1.biological <=> r2.biological ?: r1.shareHousehold <=> r2.shareHousehold
                if (res != 0) {
                    return res
                }
                return compare(r1.relatedSubjectsConstraint, r2.relatedSubjectsConstraint)
            case NullConstraint:
                return ((NullConstraint) c1).field <=> ((NullConstraint) c2).field
            case BiomarkerConstraint:
                return c1.toJson() <=> c2.toJson()
            case ModifierConstraint:
            case FieldConstraint:
            case ValueConstraint:
            case TimeConstraint:
            case StudyNameConstraint:
                return ((Comparable)c1) <=> c2
            case PatientSetConstraint:
                def p1 = (PatientSetConstraint)c1
                def p2 = (PatientSetConstraint)c2
                def res = compareNullable(p1.patientSetId, p2.patientSetId)
                if (res != 0) {
                    return res
                }
                res = compareValueList(p1.patientIds.toList(), p2.patientIds.toList())
                if (res != 0) {
                    return res
                }
                return compareValueList(p1.subjectIds.toList(), p2.subjectIds.toList())
            case ConceptConstraint:
                def conceptConstraint1 = (ConceptConstraint)c1
                def conceptConstraint2 = (ConceptConstraint)c2
                def res = compareNullable(conceptConstraint1.conceptCode, conceptConstraint2.conceptCode) ?:
                        compareNullable(conceptConstraint1.path, conceptConstraint2.path)
                if (res != 0) {
                    return res
                }
                return compareValueList(conceptConstraint1.conceptCodes, conceptConstraint2.conceptCodes)
            default:
                throw new QueryBuilderException("Constraint type not supported: ${c1.class}.")
        }
    }

    /**
     * Returns 0 if c1 and c2 are equal, -1 if c1 < c2, 1 if c1 > c2.
     * @param c1 a constraint.
     * @param c2 another constraint.
     * @return 0, -1, or 1.
     */
    @Override
    int compare(Constraint c1, Constraint c2) {
        if (c1 == null && c2 == null) {
            return 0
        } else if (c1 == null) {
            return -1
        } else if (c2 == null) {
            return 1
        }
        int typeIndex1 = indexForType(c1.class)
        int typeIndex2 = indexForType(c2.class)
        if (typeIndex1 < typeIndex2) {
            return -1
        } else if (typeIndex1 > typeIndex2) {
            return 1
        }
        return compareSameType(c1, c2)
    }
}
