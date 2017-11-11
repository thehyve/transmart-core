/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest

interface Operator {
    String LESS_THAN = '<',
           GREATER_THAN = '>',
           EQUALS = '=',
           NOT_EQUALS = '!=',
           LESS_THAN_OR_EQUALS = '<=',
           GREATER_THAN_OR_EQUALS = '>=',
           LIKE = 'like',
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

interface QueryType {
    String MIN = 'min',
           MAX = 'max',
           AVERAGE = 'average',
           COUNT = 'count',
           EXISTS = 'exists',
           NONE = 'none'
}

interface ValueType {
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


interface constraints {
    String TrueConstraint = 'true',
           BiomarkerConstraint = 'biomarker',
           ModifierConstraint = 'modifier',
           FieldConstraint = 'field',
           ValueConstraint = 'value',
           TimeConstraint = 'time',
           PatientSetConstraint = 'patient_set',
           Negation = 'negation',
           Combination = 'combination',
           TemporalConstraint = 'temporal',
           ConceptConstraint = 'concept',
           NullConstraint = 'null',
           StudyNameConstraint = 'study_name',
           SubSelectionConstraint = 'subselection',
           RelationConstraint = 'relation'
}

