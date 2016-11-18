package org.transmartproject.search.indexing.modules

import annotation.AmTagAssociation
import annotation.AmTagItem
import com.google.common.collect.ImmutableSet
import org.hibernate.SQLQuery
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.search.indexing.*

import java.util.regex.Pattern

import static AbstractFacetsIndexingFolderModule.FIELD_SUBTYPE
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_FOLDER_ID
import static org.transmartproject.search.indexing.modules.AbstractFacetsIndexingFolderModule.FOLDER_DOC_TYPE

/**
 * Indexes tag values. Each tag item has its own field in the index.
 */
@Component
class BrowseTagsIndexingModule implements FacetsIndexingModule {

    public static final int TAG_FIELDS_PRECEDENCE = FacetsFieldDisplaySettings.BROWSE_FIELDS_DEFAULT_PRECEDENCE + 1000

    private static final String FIELD_NAME_PREFIX = 'tag_'
    private static final boolean HIDE_FROM_LISTINGS = false
    public static final int NO_DISPLAY_ORDER_PENALTY = 10000

    @Autowired
    private SessionFactory sessionFactory

    private static final String QUERY_ALL = '''
        SELECT
            subject_uid AS unique_id,
            ati.code_type_name,
            ati.display_name,
            ati.display_order,
            bdv.name
        FROM
            amapp.am_tag_association ata
            INNER JOIN amapp.am_tag_item ati ON (ata.tag_item_id = ati.tag_item_id)
            LEFT JOIN biomart_user.bio_data_values_view bdv ON (ata.object_uid = bdv.unique_id)
        WHERE object_type = 'AM_TAG_VALUE'
        ORDER BY unique_id
    '''

    private static final String QUERY_SPECIFIC = """
        SELECT * FROM ($QUERY_ALL) AS A WHERE unique_id IN (:unique_ids)
    """

    final String name = 'browse_tags'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(FOLDER_DOC_TYPE)

    @Override
    Iterator<FacetsDocId> fetchAllIds(String type) {
        if (type !=  FOLDER_DOC_TYPE) {
            return [] as Set
        }

        AmTagAssociation.createCriteria().list {
            projections {
                distinct('subjectUid')
            }
        }.collect {
            new FacetsDocId(FOLDER_DOC_TYPE, it - ~/\AFOL:/)
        }.iterator()
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        def uniqueIds = docIds
                .findAll { it.type == FOLDER_DOC_TYPE }
                .collect { "FOL:${it.id}".toString() }

        if (!docIds) {
            return [] as Set
        }

        SQLQuery query = sessionFactory.currentSession.createSQLQuery(QUERY_SPECIFIC)
        query.setParameterList('unique_ids', uniqueIds)
        query.list().groupBy {
            it[0] /* folder */
        }.collect { folder, listOfRows ->
            convert folder, listOfRows
        }
    }

    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        if (!(fieldName =~ ('\\A' + Pattern.quote(FIELD_NAME_PREFIX)))) {
            return null
        }

        AmTagItem item =
                AmTagItem.findAllByCodeTypeName(
                        fieldName[(FIELD_NAME_PREFIX.size())..-3].toUpperCase(Locale.ENGLISH))[0]
        if (!item) {
            return null
        }

        new SimpleFacetsFieldDisplaySettings(
                item.displayName,
                HIDE_FROM_LISTINGS,
                TAG_FIELDS_PRECEDENCE + (item.displayOrder ?: NO_DISPLAY_ORDER_PENALTY))
    }

    private createField(String tagTypeName, String tagItemDescription, Integer displayOrder) {
        def tagTypeNameForIndex = tagTypeName
                .toLowerCase(Locale.ENGLISH)

        def fieldSuffix = tagTypeName =~ /\ANUMBER_OF_/ ? '_i' : '_t'
        FacetsFieldImpl.create(
                FIELD_NAME_PREFIX + tagTypeNameForIndex + fieldSuffix,
                HIDE_FROM_LISTINGS,
                tagItemDescription,
                TAG_FIELDS_PRECEDENCE  + (displayOrder ?: NO_DISPLAY_ORDER_PENALTY))
    }

    private convert(String folderUniqueId, List<Object[]> rows) {
        def builder = FacetsDocument.newFieldValuesBuilder()
        Long folderId = folderUniqueId.replaceFirst(/\AFOL:/, '') as Long

        builder[FIELD_FOLDER_ID] = folderId
        rows.each {
            def (dummy, tagTypeName, tagItemDescription, displayOrder, tagValue) = it

            def field = createField(tagTypeName, tagItemDescription, displayOrder)
            if (field.type == FacetsFieldType.INTEGER && tagValue?.isLong()) {
                tagValue = tagValue as Long
            }
            builder[field] = tagValue
        }

        new FacetsDocument(
                facetsDocId: new FacetsDocId(FOLDER_DOC_TYPE, folderId as String),
                fieldValues: builder.build())
    }
}
