/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

public interface Operator {
    String LESS_THAN = '<',
           GREATER_THAN = '>',
           EQUALS ='=',
           NOT_EQUALS = '!=',
           LESS_THAN_OR_EQUALS = '<=',
           GREATER_THAN_OR_EQUALS = '>=',
           LIKE ='like',
           CONTAINS = 'contains',
           IN = 'in',
           BEFORE = '<-',
           AFTER = '->',
           BETWEEN = '<-->',
           AND = 'and',
           OR = 'or',
           NOT = 'not',
           EXISTS = 'exists',
           NONE = 'none'
}

public interface QueryType {
    String VALUES = 'values',
           MIN = 'min',
           MAX = 'max',
           AVERAGE = 'average',
           COUNT = 'count',
           EXISTS = 'exists',
           NONE = 'none'
}

public interface ValueType {
    String ID = 'ID',
           NUMERIC = 'NUMERIC',
           DATE = 'DATE',
           STRING = 'STRING',
           EVENT = 'EVENT',
           OBJECT = 'OBJECT',
           COLLECTION = 'COLLECTION',
           CONSTRAINT = 'CONSTRAINT',
           NONE = 'NONE'
}



public interface constraints {
    String TrueConstraint = 'TrueConstraint',
           BiomarkerConstraint = 'BiomarkerConstraint',
           ModifierConstraint = 'ModifierConstraint',
           FieldConstraint = 'FieldConstraint',
           ValueConstraint = 'ValueConstraint',
           TimeConstraint = 'TimeConstraint',
           PatientSetConstraint = 'PatientSetConstraint',
           Negation = 'Negation',
           Combination = 'Combination',
           TemporalConstraint = 'TemporalConstraint',
           ConceptConstraint = 'ConceptConstraint',
           NullConstraint = 'NullConstraint',
           StudyNameConstraint = 'StudyNameConstraint'
}

