package transmart.solr.indexing

import groovy.util.logging.Log4j
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.apache.http.params.HttpParams
import org.apache.http.protocol.HttpContext
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.grails.core.AbstractGrailsApplication
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit

@Component
@Log4j
class SolrFacetsCore implements DisposableBean {

    public static final int MAX_CONNECTIONS = 25
    public static final int TIMEOUT_IN_SECONDS = 25

    @Autowired
    private AbstractGrailsApplication grailsApplication

    @Delegate(interfaces=false)
    private HttpSolrServer delegate

    private Thread evictionThread

    private getBaseUrl() {
        def c = grailsApplication.config.com.rwg.solr
        def path = c.facets.path ?: c.browse.path.split('/').findAll()
                .reverse().drop(2).reverse() // drop two last elements (rwg, select)
                .plus('facets').join('/')
        "${c.scheme}://${c.host}/$path"
    }

    @PostConstruct
    private void init() {
        final ClientConnectionManager manager = new PoolingClientConnectionManager()
        manager.defaultMaxPerRoute = MAX_CONNECTIONS
        manager.maxTotal = MAX_CONNECTIONS


        DefaultHttpClient httpClient = new DefaultHttpClient(manager)
        httpClient.httpRequestRetryHandler = new HttpRequestRetryHandler() {
            @Override
            boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
               executionCount < 3
            }
        }

        // Provide eviction thread to clear out stale threads.
        evictionThread = Thread.startDaemon 'solrFacetsConnectionEviction', {
            try {
                Thread thread = Thread.currentThread()
                while (true) {
                    synchronized(thread) {
                        thread.wait 5000
                        manager.closeExpiredConnections()
                        manager.closeIdleConnections(30, TimeUnit.SECONDS)
                    }
                }
            } catch (final InterruptedException ex) {
            }
        }

        final HttpParams params = httpClient.params
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_IN_SECONDS * 1000)
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_IN_SECONDS * 1000)
        params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true) // ! performance penalty
        params.setParameter(CoreConnectionPNames.SO_KEEPALIVE, true)

        delegate = new HttpSolrServer(baseUrl, httpClient)
    }

    SolrServer getSolrServer() {
        delegate
    }

    @Override
    void destroy() {
        evictionThread.interrupt()
        delegate.httpClient.connectionManager.shutdown()
    }
}
