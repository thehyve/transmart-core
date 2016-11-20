package org.transmartproject.search.indexing

import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.solr.common.SolrException
import org.junit.internal.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule skipping the tests if solr is not available.
 */
@Log4j
class SkipIfSolrNotAvailableRule implements TestRule {

    def cachedResult

    @Override
    Statement apply(Statement base, Description description) {
        try {
            if (cachedResult == true) {
                return base
            } else if (cachedResult instanceof Exception) {
                throw cachedResult
            }

            Holders.applicationContext.getBean(SolrFacetsCore).ping()
            cachedResult = true
            base
        } catch (IOException|SolrException ioe) {
            cachedResult = ioe
            new Statement() {
                @Override
                void evaluate() throws Throwable {
                    throw new AssumptionViolatedException("Solr is not running: ${ioe.message}", ioe)
                }
            }
        }
    }
}
