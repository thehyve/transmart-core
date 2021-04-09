/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.pedigree

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = [ 'leftSubject', 'relationType', 'rightSubject' ])
class Relation implements org.transmartproject.core.pedigree.Relation, Serializable {

    PatientDimension leftSubject
    RelationType relationType
    PatientDimension rightSubject
    Boolean biological
    Boolean shareHousehold

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id composite: [ 'leftSubject', 'relationType', 'rightSubject' ]
        leftSubject column: 'left_subject_id'
        relationType column: 'relation_type_id'
        rightSubject column: 'right_subject_id'
        version false
    }

    static constraints = {
        leftSubject nullable: false
        relationType nullable: false
        rightSubject nullable: false
        biological nullable: true
        shareHousehold nullable: true
    }
}
