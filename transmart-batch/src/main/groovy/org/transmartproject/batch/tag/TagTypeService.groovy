package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables

import java.sql.Types
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Service for inserting and fetching tag types.
 */
@Component
@JobScope
@Slf4j
class TagTypeService {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value(Tables.I2B2_TAG_TYPES)
    SimpleJdbcInsert insertTagTypes

    @Value(Tables.I2B2_TAG_OPTIONS)
    SimpleJdbcInsert insertTagOptions

    private static class TagTypeToIdMapper implements RowMapper<String> {

        Map<String, Integer> tagTypeToIdMap = [:]

        @Override
        String mapRow(ResultSet rs, int rowNum) throws SQLException {
            String tagType = rs.getString('tag_type')
            Integer tagTypeId = rs.getInt('tag_type_id')
            tagTypeToIdMap[tagType] = tagTypeId
            log.debug("Add mapping: ${tagType} -> ${tagTypeId}")
            tagType
        }

    }

    private static class TagTypeToOptionsMapper implements RowMapper<String> {

        Map<String, Map<String, Integer>> tagTypeToOptionsMap = [:]

        @Override
        String mapRow(ResultSet rs, int rowNum) throws SQLException {
            String tagType = rs.getString('tag_type')
            String value = rs.getString('value')
            Integer optionId = rs.getInt('tag_option_id')
            def options = tagTypeToOptionsMap[tagType]
            if (options == null) {
                tagTypeToOptionsMap[tagType] = [:] as Map<String, Integer>
                options = tagTypeToOptionsMap[tagType]
            }
            options.put(value, optionId)
            log.debug("Add value '${value}' (${optionId}) for tag type ${tagType}")
            value
        }

    }

    private static class ResultSetToTagTypeMapper implements RowMapper<TagType> {

        Map<String, TagType> tagTypes = [:]

        @Override
        TagType mapRow(ResultSet rs, int rowNum) throws SQLException {
            def tagType = rs.getString('tag_type')
            def type = tagTypes[tagType]
            if (type == null) {
                type = new TagType(
                        id: rs.getInt('tag_type_id'),
                        nodeType: rs.getString('node_type'),
                        title: rs.getString('tag_type'),
                        solrFieldName: rs.getString('solr_field_name'),
                        valueType: rs.getString('value_type'),
                        values: [],
                        shownIfEmpty: rs.getBoolean('shown_if_empty'),
                        index: rs.getInt('index')
                )
                tagTypes[tagType] = type
            }
            def value = rs.getString('value')
            type.values.add(value)
            log.debug("Add value '${value}' for tag type ${tagType}")
            type
        }

    }

    /**
     * Fetches tag types options and builds a map from tag type to the list of associated
     * options.
     *
     * @return the map from tag types to their associated options.
     */
    def fetchAllTagTypeOptions() {
        def tagTypeToOptionsMapper = new TagTypeToOptionsMapper()
        jdbcTemplate.query(
                "SELECT * FROM ${Tables.I2B2_TAG_TYPES} t " +
                        "JOIN ${Tables.I2B2_TAG_OPTIONS} o ON t.tag_type_id = o.tag_type_id".toString(),
                tagTypeToOptionsMapper
        )
        tagTypeToOptionsMapper.tagTypeToOptionsMap
    }

    def fetchAllTagTypes() {
        def resultSetToTagTypeMapper = new ResultSetToTagTypeMapper()
        jdbcTemplate.query(
                "SELECT * FROM ${Tables.I2B2_TAG_TYPES} t " +
                        "JOIN ${Tables.I2B2_TAG_OPTIONS} o ON t.tag_type_id = o.tag_type_id".toString(),
                resultSetToTagTypeMapper
        )
        resultSetToTagTypeMapper.tagTypes
    }

    def fetchAllTagTypeOptionsWithReferences() {
        def tagTypeToOptionsMapper = new TagTypeToOptionsMapper()
        jdbcTemplate.query(
                "SELECT * FROM ${Tables.I2B2_TAG_TYPES} t " +
                        "JOIN ${Tables.I2B2_TAG_OPTIONS} o ON t.tag_type_id = o.tag_type_id " +
                        "JOIN ${Tables.I2B2_TAGS} tags ON tags.tag_option_id = o.tag_option_id".toString(),
                tagTypeToOptionsMapper
        )
        tagTypeToOptionsMapper.tagTypeToOptionsMap
    }

    def deleteTagTypes(List<TagType> tagTypes) {
        def optionsDeleteList = tagTypes.collect { tagType ->
            "DELETE FROM ${Tables.I2B2_TAG_OPTIONS} WHERE tag_type_id = ${tagType.id}".toString()
        }
        if (!optionsDeleteList.empty) {
            def optionsCounts = jdbcTemplate.batchUpdate(optionsDeleteList as String[]) as Collection<Integer>
            log.info("Deleted ${optionsCounts.sum()} current tag options.")
        }
        def tagTypesDeleteList = tagTypes.collect { tagType ->
            "DELETE FROM ${Tables.I2B2_TAG_TYPES} WHERE tag_type_id = ${tagType.id}".toString()
        }
        def tagTypesCount = 0
        if (!tagTypesDeleteList.empty) {
            def tagTypesCounts = jdbcTemplate.batchUpdate(tagTypesDeleteList as String[]) as Collection<Integer>
            tagTypesCount = tagTypesCounts.sum()
            log.info("Deleted ${tagTypesCount} current tag types.")
        }
        tagTypesCount
    }

    int deleteTagTypeOptions(List<Integer> tagOptionsIds) {
        def optionsDeleteList = tagOptionsIds.collect { tagOptionsId ->
            "DELETE FROM ${Tables.I2B2_TAG_OPTIONS} WHERE tag_option_id = ${tagOptionsId}".toString()
        }
        def optionsCount = 0
        if (!optionsDeleteList.empty) {
            def optionsCounts = jdbcTemplate.batchUpdate(optionsDeleteList as String[]) as Collection<Integer>
            optionsCount = optionsCounts.sum()
            log.debug("Deleted ${optionsCounts.sum()} current tag options.")
        }
        optionsCount
    }

    static final PARAM_TYPES = [
            Types.VARCHAR,
            Types.VARCHAR,
            Types.VARCHAR,
            Types.BIT,
            Types.INTEGER,
    ] as int[]

    int updateTagTypeProperties(TagTypeUpdate update) {
        log.debug("Updating properties of tag type ${update.oldType.title}.")
        def tagType = update.newType
        def sql = "UPDATE ${Tables.I2B2_TAG_TYPES} SET " +
                'solr_field_name = ?, ' +
                'node_type = ?, ' +
                'value_type = ?, ' +
                'shown_if_empty = ?, ' +
                '"index" = ? ' +
                "WHERE tag_type_id = ${update.oldType.id}".toString()
        def params = [
                tagType.solrFieldName,
                tagType.nodeType,
                tagType.valueType,
                tagType.shownIfEmpty,
                tagType.index
        ] as Object[]
        jdbcTemplate.update(sql, params, PARAM_TYPES)
    }

    /**
     * Update an existing tag types in two stages (per type):
     * 1. update the tag type options;
     * 2. update the tag type properties.
     *
     * The update fails if tag type options are deleted that still
     * have references to them.
     *
     * @param updates
     */
    def updateTagTypes(List<TagTypeUpdate> updates) throws InvalidTagTypeOptionsDeleteException {
        def tagTypeOptionsWithReferences = fetchAllTagTypeOptionsWithReferences()
        // check if tag type options can be safely deleted.
        updates.each { TagTypeUpdate update ->
            def referencedOptions = (tagTypeOptionsWithReferences[update.oldType.title] ?: [:]).keySet()
            def deletedOptions = update.deletedOptions() as Set
            if (!deletedOptions.disjoint(referencedOptions)) {
                def invalidDeletes = deletedOptions.intersect(referencedOptions)
                throw new InvalidTagTypeOptionsDeleteException(
                        "Cannot delete options from tag type '${update.oldType.title}', " +
                        "because of existing references: ${invalidDeletes.join(', ')}"
                )
            }
        }
        // apply updates
        def tagTypeToOptionsMap = fetchAllTagTypeOptions()
        int deleteCount = 0
        int insertCount = 0
        int updateCount = 0
        updates.each { TagTypeUpdate update ->
            // delete tag types options
            def tagTypeOptions = tagTypeToOptionsMap[update.oldType.title]
            def deletedOptions = update.deletedOptions()
            def tagOptionsIds = deletedOptions.collect { option -> tagTypeOptions[option] } as Collection<Integer>
            deleteCount += deleteTagTypeOptions(tagOptionsIds)
            // add tag type options
            def addedOptions = update.addedOptions()
            insertCount += addTagTypeOptions(update.oldType, addedOptions)
            // update properties
            updateCount += updateTagTypeProperties(update)
        }
        log.info "Deleted ${deleteCount} tag type options."
        log.info "Added ${insertCount} tag type options."
        log.info "Updated ${updateCount} tag types."
        deleteCount + insertCount + updateCount
    }

    def getInsertTagOptionsTemplate() {
        def insertTemplate = insertTagOptions
        if (insertTemplate.columnNames.empty) {
            insertTemplate = insertTemplate.usingColumns(
                    'tag_type_id',
                    'value'
            )
        }
        insertTemplate
    }

    int addTagTypeOptions(TagType tagType, Collection<String> values) {
        assert tagType.id != null
        log.debug "Tag type: ${tagType}"
        int count = 0
        if (!values.empty) {
            def counts = insertTagOptionsTemplate.executeBatch(
                    values.collect { String value ->
                        log.debug "Value: ${value}"
                        [tag_type_id: tagType.id,
                         value      : value
                        ]
                    }.toArray() as Map<String, Object>[]
            ) as Collection<Integer>
            count = counts.sum()
            log.debug("Added ${count} tag options.")
        }
        count
    }

    def insert(List<? extends TagType> items) {
        insertTagTypes.usingColumns(
                'tag_type',
                'solr_field_name',
                'node_type',
                'value_type',
                'shown_if_empty',
                '"index"'
        ).executeBatch(
                items.collect { TagType tagType ->
                    log.debug "Tag type: ${tagType}"
                    [tag_type: tagType.title,
                     solr_field_name: tagType.solrFieldName,
                     node_type: tagType.nodeType,
                     value_type: tagType.valueType,
                     shown_if_empty: tagType.shownIfEmpty,
                     '"index"': tagType.index
                    ]
                }.toArray() as Map<String, Object>[]
        )

        def tagTypeToIdMapper = new TagTypeToIdMapper()

        jdbcTemplate.query(
                "SELECT * FROM ${Tables.I2B2_TAG_TYPES}".toString(),
                tagTypeToIdMapper
        )

        insertTagOptionsTemplate.executeBatch(
                items.collectMany { TagType tagType ->
                    log.debug "Tag type: ${tagType}"
                    tagType.values.collect { String value ->
                        log.debug "Value: ${value}"
                        [tag_type_id: tagTypeToIdMapper.tagTypeToIdMap[tagType.title],
                         value: value
                        ]
                    }
                }.toArray() as Map<String, Object>[]
        )
    }

}
