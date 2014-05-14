package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.Study

class I2b2 extends AbstractI2b2Metadata implements Serializable {

    BigDecimal   cTotalnum
    String       cComment
    String       mAppliedPath
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    String       valuetypeCd
    String       mExclusionCd
    String       cPath
    String       cSymbol

    static String backingTable = 'I2B2'

    static mapping = {
        table         name: 'I2B2', schema: 'I2B2METADATA'
        version       false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id          composite: ['fullName', 'name']

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
    }

    static constraints = {
        cTotalnum           nullable:   true
        cComment            nullable:   true
        mAppliedPath        nullable:   false,   maxSize:   700
        downloadDate        nullable:   true
        updateDate          nullable:   false
        importDate          nullable:   true
        sourcesystemCd      nullable:   true,    maxSize:   50
        valuetypeCd         nullable:   true,    maxSize:   50
        mExclusionCd        nullable:   true,    maxSize:   25
        cPath               nullable:   true,    maxSize:   700
        cSymbol             nullable:   true,    maxSize:   50

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
    }

    @Override
    Study getStudy() {
        def trial

        def matcher = cComment =~ /(?<=^trial:).+/
        if (matcher.find()) {
            trial = matcher.group 0
        }

        if (!trial) {
            return null
        }

        def query = sessionFactory.currentSession.createQuery '''
                SELECT I
                FROM I2b2 I, I2b2TrialNodes TN
                WHERE (I.fullName = TN.fullName) AND
                TN.trial = :trial'''

        query.setParameter 'trial', trial

        def res = query.list()
        if (res.size() > 1) {
            throw new UnexpectedResultException("More than one study with name $trial")
        } else if (res.size() == 1) {
            new StudyImpl(ontologyTerm: res[0])
        } else {
            null
        }
    }

}
