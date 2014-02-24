package org.transmartproject.db.dataquery.highdim.mirna

import grails.test.mixin.TestMixin
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

@TestMixin(RuleBasedIntegrationTestMixin)
class MirnaSeqEndToEndRetrievalTests extends MirnaSharedEndToEndRetrievalTests {
    String typeName = 'mirnaseq'
}
