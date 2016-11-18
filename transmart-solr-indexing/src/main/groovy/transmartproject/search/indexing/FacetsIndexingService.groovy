package org.transmartproject.search.indexing

import com.google.common.collect.HashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.solr.client.solrj.request.AbstractUpdateRequest
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.common.SolrException
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.SolrInputField
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptFullName

import static org.transmartproject.search.indexing.FacetsFieldImpl.create
import static org.transmartproject.search.indexing.FacetsFieldImpl.create

@Component
@Log4j
class FacetsIndexingService implements InitializingBean {

    public static final String FIELD_NAME_ID = 'id'
    public static final String FIELD_NAME_TYPE = 'TYPE'
    public static final String FIELD_NAME_FILE_PATH = 'FILE_PATH'
    public static final String FIELD_NAME_FOLDER_ID = 'FOLDER_ID'
    public static final String FIELD_NAME_SUBTYPE = 'SUBTYPE'
    public static final String FIELD_NAME_TEXT = 'TEXT'
    public static final String FIELD_NAME_CONCEPT_PATH = 'CONCEPT_PATH'
    public static final String FIELD_NAME__VERSION_ = '_version_'

    public static final int CONCEPT_PATH_FIELD_PRECEDENCE = -1

    public static final FacetsFieldImpl FIELD_TEXT = create(
            FIELD_NAME_TEXT, false, 'Full Text', Ordered.HIGHEST_PRECEDENCE, FacetsFieldType.TEXT)
    public static final FacetsFieldImpl FIELD_FOLDER_ID = create(
            FIELD_NAME_FOLDER_ID, true, 'Folder id', Ordered.LOWEST_PRECEDENCE, FacetsFieldType.INTEGER)
    public static final FacetsFieldImpl FIELD_CONCEPT_PATH = create(
            FIELD_NAME_CONCEPT_PATH, false, 'Concept path', CONCEPT_PATH_FIELD_PRECEDENCE, FacetsFieldType.STRING)
    public static final FacetsFieldImpl FIELD_SUBTYPE = create(
            FIELD_NAME_SUBTYPE, true, 'Subtype', Ordered.LOWEST_PRECEDENCE, FacetsFieldType.STRING)


    public static final String FIELD_NAME_TITLE = 'title_t'
    public static final String FIELD_NAME_CONTENT_TYPE = 'content_type_s'
    public static final String FIELD_NAME_LAST_MODIFIED = 'last_modified_d'
    public static final String FIELD_NAME_TIMESTAMP = 'timestamp_d'

    int commitChunkSize = 500
    int queryChunkSize = 500

    private boolean uncommittedDeletions = false
    private List<SolrInputDocument> uncommittedDocuments = Lists.newArrayListWithCapacity(commitChunkSize)
    private List<SolrInputDocument> uncommittedWithFiles = Lists.newArrayListWithCapacity(commitChunkSize)

    private def useMongo = Holders.config.transmartproject.mongoFiles.enableMongo

    @Autowired
    private SolrFacetsCore solrFacetsCore

    @Autowired
    private List<FacetsIndexingModule> modules

    void clearIndex() {
        solrFacetsCore.deleteByQuery '*:*'
        solrFacetsCore.commit()
    }

    void removeDocuments(Set<FacetsDocId> documents) {
        uncommittedDeletions = true
        solrFacetsCore.deleteById(documents*.toString())
    }

    void fullIndex() {
        indexByTypes(null)
    }

    private void index(Multimap<FacetsDocId, FacetsIndexingModule> idModuleMap) {
        (idModuleMap.keySet() as List).collate(queryChunkSize).each { List<FacetsDocId> idsList ->
            Multimap<FacetsDocId, FacetsDocument> docsPerId = HashMultimap.create()
            Multimap<FacetsIndexingModule, FacetsDocId> idsPerModule = HashMultimap.create()

            idsList.each { FacetsDocId docId ->
                idModuleMap.get(docId).each { FacetsIndexingModule mod ->
                    idsPerModule.put(mod, docId)
                }
            }

            idsPerModule.keySet().each { FacetsIndexingModule mod ->
                mod.collectDocumentsWithIds(idsPerModule.get(mod) as Set).each { FacetsDocument it ->
                    docsPerId.put(it.facetsDocId, it)
                }
            }

            docsPerId.keySet().each { id ->
                addDocument FacetsDocument.merge(docsPerId.get(id))
            }
        }
        flush()
    }

    private index(FacetsIndexingModule module, Set<FacetsDocId> ids) {
        module.collectDocumentsWithIds(ids).each {
            addDocument it
        }
        flush()
    }


    void indexByTypes(Set<String> types) {
        if (types == null) {
            types = modules.collectMany { it.supportedDocumentTypes } as Set
        }

        types.each { String type ->
            Multimap<FacetsDocId, FacetsIndexingModule> ids = HashMultimap.create()

            def supportingModules = modules.findAll { type in it.supportedDocumentTypes }

            // optimization, we don't need to merge documents
            if (supportingModules.size() == 1) {
                def mod = supportingModules[0]
                mod.fetchAllIds(type).eachWithIndex { FacetsDocId entry, int i ->
                    ids.put(entry, mod)
                    if ((i + 1) % queryChunkSize == 0) {
                        index mod, ids.keySet()
                        ids.clear()
                    }
                }
                if (!ids.empty) {
                    index ids
                }
            } else {
                supportingModules.each { FacetsIndexingModule mod ->
                    mod.fetchAllIds(type).each {
                        ids.put(it, mod)
                    }
                }
                index ids
            }
        }
    }

    void indexByIds(Set<FacetsDocId> ids) {
        Multimap<FacetsDocId, FacetsIndexingModule> moduleIds = new HashMultimap<>()

        modules.each { mod ->
            def thisModTypes = mod.supportedDocumentTypes
            Set<FacetsDocId> idsForThisModule = ids.findAll {
                it.type in thisModTypes
            }

            idsForThisModule.each {
                moduleIds.put(it, mod)
            }
        }

        index moduleIds
    }

    void addDocument(FacetsDocument doc) {
        addDocumentInternal(convertFacetsDocument(doc))
    }

    private void addDocumentInternal(SolrInputDocument doc) {
        assert doc != null
        logException 'adding document', { solrFacetsCore.add doc }

        uncommittedDocuments << doc
        if (doc.containsKey(FIELD_NAME_FILE_PATH)) {
            uncommittedWithFiles << doc
        }
        if (uncommittedDocuments.size() >= commitChunkSize) {
            flush()
        }
    }

    void flush() {
        if (uncommittedDocuments.empty && !uncommittedDeletions) {
            log.debug 'Nothing to flush'
            return
        }

        log.debug "Committing ${uncommittedDocuments.size()} Solr documents; ids: ${uncommittedDocuments*.getFieldValue('id')}"
        try {
            // allow exception to bubble up
            // don't move to file content indexing if this fails
            solrFacetsCore.commit()
            uncommittedDeletions = false

            if (!useMongo) {
                uncommittedWithFiles.each { SolrInputDocument doc ->
                    try {
                        extractDataFromFile doc
                    } catch (Exception e) {
                        log.error("Error extracting data from file in doc $doc", e)
                    }
                }
            }
        } finally {
            /* unconditionally unqueue the documents, even if they could
             * not be indexed */
            uncommittedDocuments = Lists.newArrayListWithCapacity(commitChunkSize)
            uncommittedWithFiles = Lists.newArrayListWithCapacity(commitChunkSize)
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Loaded modules: $modules")
        def duplicateModules = modules
                .groupBy { it.name }
                .findAll { k, v -> v.size() > 1 }

        if (duplicateModules) {
            throw new IllegalStateException(
                    "Found modules with repeated names: $duplicateModules")
        }
    }

    private logException(String activity, Closure closure) {
        try {
            closure.call()
        } catch (SolrException | IOException e) {
            log.error "Error $activity: $e.message", e
        }
    }

    private extractDataFromFile(SolrInputDocument doc) {
        def req = new ContentStreamUpdateRequest('/update/extract')
        req.setParam("literal.${FIELD_NAME_ID}", doc.getField(FIELD_NAME_ID).firstValue)

        doc.iterator().each { SolrInputField f ->
            req.setParam("literal.${f.name}", f.firstValue.toString())
        }
        File f = new File(doc.getField(FIELD_NAME_FILE_PATH).firstValue.toString())
        req.setParam 'fmap.title', FIELD_NAME_TITLE
        req.setParam 'fmap.content_type', FIELD_NAME_CONTENT_TYPE
        req.setParam 'fmap.last_modified', FIELD_NAME_LAST_MODIFIED
        req.setParam 'fmap.timestamp', FIELD_NAME_TIMESTAMP
        req.setParam 'fmap.content', FIELD_TEXT.fieldName
        req.setParam 'resource.name', f.name
        req.setParam 'defaultField', FIELD_TEXT.fieldName
        req.addFile(new File(doc.getField(FIELD_NAME_FILE_PATH).firstValue.toString()), '')

        req.setAction(
                AbstractUpdateRequest.ACTION.COMMIT,
                true /* waitFlush */,
                true /* waitSearcher */)

        logException "Extracting data for document $doc", {
            solrFacetsCore.request req
        }
    }

    private SolrInputDocument convertFacetsDocument(FacetsDocument doc) {
        SolrInputDocument inputDoc = new SolrInputDocument()
        doc.fieldValues.entries().each { Map.Entry<FacetsFieldImpl, Object> entry ->
            FacetsFieldImpl fs = entry.key
            Object obj = entry.value

            if (!fs.valid) {
                log.warn("Provided document with an invalid " +
                        "field type (name doesn't match type suffix): $fs.")
            }
            if (!fs.type.isValueCompatible(obj)) {
                log.warn("Provided document with value for field $fs " +
                        "that is incompatible: '$obj' (class ${obj.getClass()}, expected " +
                        "${fs.type.getRequiredClass()}. This value will be skipped.")
                return
            }

            inputDoc.addField fs.fieldName, obj

            // also copy final component of concept path to TEXT
            if (fs.fieldName == FIELD_NAME_CONCEPT_PATH && entry.value) {
                inputDoc.addField(FIELD_NAME_TEXT, new ConceptFullName(entry.value)[-1])
            }
        }
        inputDoc.addField(FIELD_NAME_ID, doc.facetsDocId.toString())
        inputDoc.addField(FIELD_NAME_TYPE, doc.facetsDocId.type)

        if (doc.file) {
            inputDoc.addField(FIELD_NAME_FILE_PATH, doc.file.toString())
        }

        inputDoc
    }
}
