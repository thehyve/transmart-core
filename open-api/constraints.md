#Constraints
Constraints are used to build queries and are required in the v2 rest API. They consist of a Type and that type's specific arguments. They live in org.transmartproject.db.multidimquery.query.Constraint.groovy

['Combination'](#Combination)
['StudyNameConstraint',](#StudyNameConstraint)
['ConceptConstraint',](#ConceptConstraint)
['ValueConstraint',](#ValueConstraint)
['FieldConstraint',](#FieldConstraint)
['TimeConstraint',](#TimeConstraint)
['PatientSetConstraint',](#PatientSetConstraint)
['TemporalConstraint',](#TemporalConstraint)
['NullConstraint'](#NullConstraint)
['BiomarkerConstraint',](#BiomarkerConstraint)
['ModifierConstraint',](#ModifierConstraint)
['Negation',](#Negation)
['TrueConstraint'](#TrueConstraint)

##<a name="Combination">Combination</a>
Most often a combination of constraints is needed to get the right result. This can be done by a constraint with type combination.
It takes an op 'operator' and a list 'args' with constraints. All args will be evaluated together on each observation. So a 'and' operator with a 'PatientSetConstraint' and a 'ConceptConstraint' will return all observations for the given concept linkt to the patientset.
However a 'and' operator with two ConceptConstraints will evaluate to an empty result, as no observation can have two concepts. This is also true even if nested with a different combination because constraints do not know scope.

Example:
```json
{"type":"Combination",
 "operator":"and",
 "args":[
    {"type":"PatientSetConstraint","patientIds":-62},
    {"type":"ConceptConstraint","path":"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"}
    ]
}
```

##<a name="StudyNameConstraint">StudyNameConstraint</a>
Evaluate if an observation is part of a particular study

Example:
```json
{
  "type":"StudyNameConstraint",
  "studyId":"EHR"
}
```

##<a name="ConceptConstraint">ConceptConstraint</a>
Evaluate if an observation is of a particular Concept. Either by 'path' or 'conceptCode'.

```json
{
  "type":"ConceptConstraint",
  "path":"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
  "conceptCode":"HR"
}
```

##<a name="ValueConstraint">ValueConstraint</a>
Evaluate if the value of an observation is within the given parameters. It needs a 'valueType', 'operator' and 'value'.
valueType: ["NUMERIC", "STRING"]
operator: ["<", ">", "=", "!=", "<=", ">=", "in", "like", "contains"]

Example:
```json
{
  "type":"ValueConstraint",
  "valueType":"NUMERIC",
  "operator":">","value":176
}
```

##<a name="FieldConstraint">FieldConstraint</a>
Evaluate if a specific field of an observation is within the given parameters. it needs a 'field', 'operator' and 'value'.
operator: ["<", ">", "=", "!=", "<=", ">=", "in", "like", "contains"]

Example:
```json
{
  "type":"FieldConstraint",
  "field":{
      "dimension":"PatientDimension",
      "fieldName":"age",
      "type":"NUMERIC"
      },
  "operator":"<",
  "value":100
}
```

##<a name="TimeConstraint">TimeConstraint</a>
Evaluate if an observation is within the specified time period. It needs a 'field' the type of which needs to be 'DATE'. It needs a time relevant 'operator' and a list of 'values'.
The list must hold one date for the before(<-) and after(->) operator. It must hold two dates for the between(<-->) operator. If the given date field for an observation is empty, the observation will be ignored.
operator: ["<-", "->", "<-->"]

Example:
```json
{
  "type":"TimeConstraint",
  "field":{
      "dimension":"StartTimeDimension",
      "fieldName":"startDate",
      "type":"DATE"
      },
  "operator":"->",
  "values":["2016-01-01T00:00:00Z"]
}
```

##<a name="PatientSetConstraint">PatientSetConstraint</a>
Evaluate if an observation is liked to a patient in the set. It needs either a 'patientSetId' or a list of 'patientIds'.

Example:
```json
{
    "type":"PatientSetConstraint",
    "patientSetId":28820
    "patientIds":[-62,-63]
}
```

##<a name="TemporalConstraint">TemporalConstraint</a>
Evaluate if an observation happened before or after an event. It needs an 'operator' and an 'eventConstraint'. Any constraint can be used as an eventConstraint. Most likely a combination.
operator: ["<-", "->", "exists"]

Example:
```json
{
    "type":"TemporalConstraint",
    "operator":"->",
    "eventConstraint":{
          "type":"ValueConstraint",
          "valueType":"NUMERIC",
          "operator":"<",
          "value":60
          }
}
```

##<a name="NullConstraint">NullConstraint</a>
Evaluate if an specific field of an observation is null. It needs a field.

Example:
```json
{
    "type":"NullConstraint",
    "field":{
        "dimension":"EndTimeDimension",
        "fieldName":"endDate",
        "type":"DATE"
        }
}
```

##<a name="BiomarkerConstraint">BiomarkerConstraint</a>
Used to evaluate hiDim observations. It needs a 'biomarkerType' and a 'params' object.
biomarkerType: ["transcripts", "genes"]

Example:
```json
{
    "type":"BiomarkerConstraint",
    "biomarkerType":"genes",
    "params":{
        "names":["TP53"]
        }
}
```

##<a name="ModifierConstraint">ModifierConstraint</a>
Evaluate if an observation is linked to the specified modifier. Optionaly if that modifier has the specific value. It must have a 'path' or 'modifierCode' and may have 'values' in the form of a ValueConstraint.

Example:
```json
{
    "type":"ModifierConstraint",
    "modifierCode":"TNS:SMPL"
    "path":"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
    "values":{
        "type":"ValueConstraint",
        "valueType":"STRING",
        "operator":"=",
        "value":"Tumor"
        }
}
```

##<a name="Negation">Negation</a>
Evaluate if for an observation the given 'arg' is false. 'arg' is a constraint

Example:
```json
{
    "type":"Negation",
    "arg":{
        "type":"PatientSetConstraint",
        "patientIds":[-62,-52,-42]
        }
}
```
returns all observations not liked to patient with id -62, -52 or -42

##<a name="TrueConstraint">TrueConstraint</a>
**!!WARNING!!** Use mainly for testing.  
The most basic of constraints. Evaluates to true for all observations. This returns all observations the requesting user has access to.

Example:
```json
{
    "type":"TrueConstraint"
}
```