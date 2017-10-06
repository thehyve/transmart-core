/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

@Grab('org.codehaus.jackson:jackson-core-asl:1.9.13')
@Grab('org.codehaus.jackson:jackson-mapper-asl:1.9.13')
import inc.oracle.BasicItem
import inc.oracle.GrantItem
import inc.oracle.Item
import inc.oracle.ItemRepository
import inc.oracle.JacksonMapperProducer
import inc.oracle.Log
import inc.oracle.SqlProducer
import org.codehaus.jackson.annotate.JsonIgnore
import groovy.sql.Sql

import java.sql.*
import java.util.logging.Level
import java.util.logging.Logger

import static java.lang.System.out


def cli = new CliBuilder()
cli.g 'Dump global objects (currently users; which must be specified with -u)', longOpt: 'global'
cli.u 'Dump objects from a user; can be specified multiple times',
        args: 1, argName: 'user', longOpt: 'user', required: true
def options = cli.parse args
if (!options) {
    Log.err 'ERR: Invalid options'
    System.exit 1
}

def sql = SqlProducer.createFromEnv()


def filterOutSynonyms = { Closure next, Item item, ItemRepository repos ->
    if (item.type == 'SYNONYM') {
        return
    }
    next item, repos
}

def filterOutUsers = { Set<String> usersToKeep, Closure next, Item item, ItemRepository repos ->
    if (!usersToKeep.contains(item.owner)) {
        return
    }
    next item, repos
}

def writeToFileHierarchy = { Set seenFiles, Item item, ItemRepository repos ->
    def topDir = new File(item.owner.toLowerCase(Locale.ENGLISH))
    File targetFile
    if (!topDir.exists()) {
        topDir.mkdir()
    }

    def hasCrossDependency = { Item it, Stack<Item> stack ->
        if (it.type == 'PROCEDURE') {
            /* Ignore these because  the worst that can happen is the procedure
             * not compiling (but we have a step where we recompile everything afterwards) */
            return false
        }
        if (stack.contains(it)) {
            Log.err "Circular dependency: $stack and back to $it"
            return false
        }
        stack << it
        def result = repos.getParents(it).any { Item parent ->
            if (parent == it) {
                /* ignore self references */
                return false
            }
            if (parent.owner != it.owner && it.type != 'GRANT') {
                Log.warn "Cross schema dependency: ${(stack + parent).join(' -> ')}"
                return true
            }
            return owner.call(parent, stack) /* owner is surrounding closure */
        }
        stack.pop()
        result
    }

    def getTargetFileForTrigger = { TriggerItem triggerItem ->
        new File(topDir, "${triggerItem.triggerTable.nameLower ?: '_misc'}.sql")
    }

    def getTargetForConstraint  = { RefConstraintItem constraintItem ->
        new File(topDir, "${constraintItem.constraintTable.nameLower}.sql")
    }

    def getTargetForIndex  = { IndexItem indexItem ->
        new File(topDir, "${indexItem.indexTable.nameLower}.sql")
    }

    def getTargetFileForSequence = { Item sequenceItem ->
        def ret = new File(topDir, '_misc.sql')
        def dependents = repos.getChildren(sequenceItem)
        if (dependents.size() == 1) {
            def dep = dependents.iterator().next()
            if (dep.type == 'TRIGGER') {
                ret = getTargetFileForTrigger dep
            }
        }
        ret
    }

    if (hasCrossDependency(item, new Stack())) {
        targetFile = new File(topDir, '_cross.sql')
    } else if (item.type == 'TABLE') {
        targetFile = new File(topDir, "${item.nameLower}.sql")
    } else if (item.type == 'VIEW' || item.type == 'MATERIALIZED_VIEW') {
        File viewsDir = new File(topDir, 'views')
        if (!viewsDir.exists()) {
            viewsDir.mkdir()
        }
        targetFile = new File(viewsDir, "${item.nameLower}.sql")
    } else if (item.type == 'FUNCTION' || item.type == 'PROCEDURE') {
        File objectDir = new File(topDir, item.type.toLowerCase() + 's')
        if (!objectDir.exists()) {
            objectDir.mkdir()
        }
        targetFile = new File(objectDir, "${item.nameLower}.sql")
    } else if (item.type == 'TRIGGER') {
        targetFile = getTargetFileForTrigger item
    } else if (item.type == 'SEQUENCE') {
        targetFile = getTargetFileForSequence item
    } else if (item.type == 'REF_CONSTRAINT') {
        targetFile = getTargetForConstraint item
    } else if (item.type == 'INDEX') {
        targetFile = getTargetForIndex item
    } else {
        targetFile = new File(topDir, '_misc.sql')
    }

    out.println "Writing ${item.type} ${item.owner}.${item.name} to $targetFile"
    Writer writer = targetFile.newWriter 'UTF-8', seenFiles.contains(targetFile)
    seenFiles << targetFile

    repos.addFileAssignment item, targetFile

    try {
        writer << "--\n-- Type: ${item.type}; Owner: ${item.owner}; Name: ${item.name}\n--"
        writer << item.data.stripIndent().replaceAll(/(?m)[ \t]+$/, '')
        writer << '\n'
    } finally {
        writer.close()
    }
}

def globalWriteToHierarchy = { Set seenFiles, Item item, ItemRepository repos ->
    def topDir = new File('GLOBAL')
    if (!topDir.exists()) {
        topDir.mkdir()
    }

    File targetFile = new File(topDir, item.nameLower + '.sql')

    out.println "Writing ${item.type} ${item.name} to $targetFile"
    Writer writer = targetFile.newWriter 'UTF-8', seenFiles.contains(targetFile)
    seenFiles << targetFile

    try {
        writer << "--\n-- Type: ${item.type}; Name: ${item.name}\n--"
        writer << item.data.stripIndent()
        writer << '\n'
    } finally {
        writer.close()
    }
}

/* {{{ Item subclasses */
class GenericDdlItem extends BasicItem {
    @JsonIgnore
    DDLFetcher ddlFetcher
    @JsonIgnore
    String ddl

    String getData() {
        if (!ddl) {
            ddl = ddlFetcher.getNamedObjectDDL this
        }
        ddl
    }
}

class TableItem extends GenericDdlItem {
    String getData() {
        def data = super.data

        data.
                replaceAll(/(\s*USING INDEX) PCTFREE.*/, '$1').
                replaceAll(/\s*PCTFREE .*/, '')
    }
}

class GrantedItem extends GenericDdlItem {
    String getData() {
        if (!ddl) {
            ddl = ddlFetcher.getGrantedDDL this
        }
        ddl
    }
}

class TriggerItem extends GenericDdlItem {
    @JsonIgnore
    Item getTriggerTable() {
        ddlFetcher.getTriggerTable this
    }
}

class RefConstraintItem extends GenericDdlItem {
    @JsonIgnore
    Item getConstraintTable() {
        ddlFetcher.getRefConstraintTable this
    }
}

class IndexItem extends GenericDdlItem {
    @JsonIgnore
    Item getIndexTable() {
        ddlFetcher.getIndexTable this
    }

    String getData() {
        def data = super.data

        data.
                replaceAll(/(\s*)PCTFREE .* LOCAL/, '$1LOCAL').
                replaceAll(/(\s*)PCTFREE .*/, '')
    }
}
/* }}} */

class ItemFactory {
    DDLFetcher ddlFetcher
    Map<Item, Item> allItems = new HashMap()

    Item createItem(Map map, String ddl = null) {
        def obj
        switch (map.type) {
            case 'TABLE':
                obj = new TableItem()
                break
            case 'INDEX':
                obj = new IndexItem()
                break
            case 'TRIGGER':
                obj = new TriggerItem()
                break
            case 'REF_CONSTRAINT':
                obj = new RefConstraintItem()
                break
            case 'ROLE_GRANT':
            case 'SYSTEM_GRANT':
            case 'TABLESPACE_QUOTA':
                obj = new GrantedItem()
                break
            case null:
                throw new RuntimeException("No name")
            case 'MATERIALIZED VIEW':
                // the type is actually without underscore, but the fetch DDL
                // functions expect an underscore
                map.type = 'MATERIALIZED_VIEW'
            default:
                obj = new GenericDdlItem()
        }

        obj.type = map.type
        obj.owner = map.owner
        obj.name = map.name
        if (allItems.get(obj)) {
            def prevObj = allItems.get(obj)
            if (ddl) {
                prevObj.ddl = ddl
            }
            return prevObj
        }

        obj.ddlFetcher = this.ddlFetcher
        obj.ddl = ddl
        allItems.put(obj, obj)
        obj
    }

}

class DDLFetcher {
    Sql sql
    private static SQL_LOGGER = Logger.getLogger(Sql.class.name)

    String getNamedObjectDDL(Item item) {
        if (item.type == 'NON-EXISTENT') {
            return ''
        }

        def res = sql.firstRow "SELECT DBMS_METADATA.GET_DDL($item.type, $item.name, $item.owner) FROM DUAL"
        Clob clob = res[0]
        clob.characterStream.text
    }

    String getGrantedDDL(GrantedItem item) {
        SQL_LOGGER.level = Level.SEVERE /* kills kittens */
        try {
            def res
            if (item.owner) {
                res = sql.firstRow """SELECT DBMS_METADATA.GET_GRANTED_DDL(
                        $item.type, $item.name, $item.owner) FROM DUAL"""
            } else {
                res = sql.firstRow "SELECT DBMS_METADATA.GET_GRANTED_DDL($item.type, $item.name) FROM DUAL"
            }
            Clob clob = res[0]
            return clob.characterStream.text
        } catch (SQLException e) {
            if (e.vendorCode == 31608) {
                //ignore, just no dependent objects of this type exist
                return ''
            } else {
                throw e
            }
        } finally {
            SQL_LOGGER.level = Level.WARNING
        }
    }

    String getDependentObjectDDL(String type, Item parentItem) {
        def res = sql.firstRow """SELECT DBMS_METADATA.GET_DEPENDENT_DDL(
                $type, $parentItem.name, $parentItem.owner) FROM DUAL"""
        Clob clob = res[0]
        clob.characterStream.text
    }

    //doesn't really belong here...
    ItemFactory factory
    Item getTriggerTable(TriggerItem item) {
        def res = sql.firstRow """SELECT TABLE_OWNER, TABLE_NAME FROM DBA_TRIGGERS WHERE
                OWNER = ${item.owner} AND TRIGGER_NAME = ${item.name}"""

        factory.createItem(owner: res[0], name: res[1], type: 'TABLE')
    }

    Item getRefConstraintTable(RefConstraintItem item) {
        def res = sql.firstRow """SELECT TABLE_NAME FROM DBA_CONSTRAINTS WHERE
                OWNER = ${item.owner} AND CONSTRAINT_NAME = ${item.name}"""

        factory.createItem(owner: item.owner, name: res['TABLE_NAME'], type: 'TABLE')
    }

    Item getIndexTable(IndexItem item) {
        def res = sql.firstRow """SELECT TABLE_NAME, TABLE_TYPE FROM DBA_INDEXES WHERE
                OWNER = ${item.owner} AND INDEX_NAME = ${item.name}"""

        factory.createItem(owner: item.owner, name: res['TABLE_NAME'], type: res['TABLE_TYPE'])
    }

}
class ObjectFinder {
    Sql sql
    ItemFactory factory
    ItemRepository repository

    void findAll(String owner) {
        owner = owner.toUpperCase()
        find_dba_dependencies(owner)
        find_dba_objects(owner)
        find_referential_constraints(owner)
        find_indexes(owner)
    }

    void findGlobal(List<String> users) {
        find_users users
    }

    private find_dba_dependencies(String owner) {
        out.println "Fetching from dba_dependencies, owner $owner..."
        // skip objects of type 'TYPE BODY' as dumping the type already dumps its body
        sql.eachRow """SELECT NAME, TYPE, REFERENCED_OWNER, REFERENCED_NAME, REFERENCED_TYPE
                FROM dba_dependencies WHERE owner = $owner
                AND referenced_owner <> 'SYS' AND referenced_owner <> 'PUBLIC'
                AND type <> 'TYPE BODY' AND name NOT LIKE 'BIN\$%'
                AND referenced_name NOT LIKE 'BIN\$%'""",
        {
            def parent = factory.createItem owner: it['REFERENCED_OWNER'],
                                            type:  it['REFERENCED_TYPE'],
                                            name:  it['REFERENCED_NAME']
            def child = factory.createItem  owner: owner,
                                            type:  it['TYPE'],
                                            name:  it['NAME']
            repository.addDependency parent, child
        }
    }

    private find_dba_objects(String owner) {
        out.println "Fetching from dba_objects, owner $owner..."
        sql.eachRow """SELECT object_type, object_name,
                dbms_metadata.get_ddl(object_type, object_name, $owner) as ddl
                FROM dba_objects
                WHERE owner = $owner AND object_type IN (
                'SEQUENCE', 'TRIGGER', 'MATERIALIZED_VIEW', 'FUNCTION',
                'TABLE', 'VIEW', 'PROCEDURE', 'TYPE')""", {
            def item = factory.createItem owner: owner,
                                          type:  it['OBJECT_TYPE'],
                                          name:  it['OBJECT_NAME'],
                                          it['DDL'].characterStream.text

            repository.addItem item
        }
    }

    private find_referential_constraints(String owner) {
        out.println "Fetching from dba_constraints, owner $owner..."
        sql.eachRow """SELECT
                           C.owner, C.constraint_name, C.table_name,
                           C.r_owner, C.r_constraint_name, D.table_name AS r_table_name,
                           dbms_metadata.get_ddl('REF_CONSTRAINT', C.constraint_name, C.owner) as ddl
                       FROM
                           dba_constraints C
                           LEFT JOIN dba_constraints D ON (
                                c.r_constraint_name = d.constraint_name and c.r_owner = d.owner)
                       WHERE C.constraint_type = 'R'
                           AND C.owner = $owner""", {
            def constraint = factory.createItem owner: owner,
                                                type:  'REF_CONSTRAINT',
                                                name:  it['CONSTRAINT_NAME'],
                                                it['DDL'].characterStream.text

            def childTable = factory.createItem owner: owner,
                                                type:  'TABLE',
                                                name:  it['TABLE_NAME']

            def parentTable = factory.createItem owner: it['R_OWNER'],
                                                 type:  'TABLE',
                                                 name:  it['R_TABLE_NAME']

            repository.addItem constraint
            repository.addDependency childTable, constraint //constraint should depend on its table
            repository.addDependency parentTable, childTable //child table should depend on parent table
        }
    }

    private find_indexes(String owner) {
        out.println "Fetching from dba_indexes, owner $owner..."
        sql.eachRow """SELECT
                           owner, index_name, table_owner, table_name, table_type,
                           dbms_metadata.get_ddl('INDEX', index_name, owner) as ddl
                       FROM dba_indexes
                       WHERE owner = $owner
                       AND uniqueness <> 'UNIQUE'""", {
            def index = factory.createItem owner: owner,
                                           type:  'INDEX',
                                           name:  it['INDEX_NAME'],
                                           it['DDL'].characterStream.text

            def parentTable = factory.createItem owner: it['TABLE_OWNER'],
                                                 type:  it['TABLE_TYPE'],
                                                 name:  it['TABLE_NAME']

            repository.addItem index
            repository.addDependency parentTable, index //index should depend on its table
        }
    }

    private find_users(List<String> users) {
        out.println "Fetching from dba_users..."
        sql.eachRow """SELECT username, dbms_metadata.get_ddl('USER', username) as ddl FROM dba_users""", {
            if (!users.contains(it['USERNAME'])) {
                return
            }

            def user = factory.createItem type: 'USER', name: it['USERNAME'], it['DDL'].characterStream.text
            def roleGrants = factory.createItem type: 'ROLE_GRANT', name: it['USERNAME']
            def systemGrants = factory.createItem type: 'SYSTEM_GRANT', name: it['USERNAME']
            def tablespaceQuota = factory.createItem type: 'TABLESPACE_QUOTA', name: it['USERNAME']

            repository.addItem user

            repository.addDependency user, roleGrants
            repository.addDependency user, systemGrants
            repository.addDependency user, tablespaceQuota
        }
    }
}

def setupTransformParams(Sql sql) {
    out.println "Executing setup statements..."
    sql.execute '''
BEGIN
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_ATTRIBUTES', TRUE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'STORAGE', FALSE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'TABLESPACE', TRUE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS', TRUE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'REF_CONSTRAINTS', FALSE);
dbms_metadata.set_transform_param(DBMS_METADATA.SESSION_TRANSFORM, 'SIZE_BYTE_KEYWORD', TRUE);
END;
'''
}

/* wiring */
def ddlFetcher = new DDLFetcher(sql: sql)
def itemFactory = new ItemFactory(ddlFetcher: ddlFetcher)
ddlFetcher.factory = itemFactory //circular dep
def repository = new ItemRepository()
def objectFinder = new ObjectFinder(sql: sql, factory: itemFactory, repository: repository)

setupTransformParams sql

def users = options.us.collect { it.toUpperCase(Locale.ENGLISH) }
def sorter
def seenFiles = new HashSet()

if (options.g) {
     objectFinder.findGlobal users
    sorter = globalWriteToHierarchy.curry seenFiles
} else {
    users.each {
        objectFinder.findAll it
    }
    def userSet = new HashSet()
    userSet.addAll users

    sorter = filterOutSynonyms.curry(
            filterOutUsers.curry(userSet,
                    writeToFileHierarchy.curry(seenFiles)))
}

out.println "Start writing files"
repository.writeWithSorter sorter

if (!options.g) {
    users.each { String user ->
        File out = new File(user.toLowerCase(Locale.ENGLISH), 'items.json')
        out.println "Writing item repository file for $user to $out"
        JacksonMapperProducer.mapper.writeValue out, repository.forUser(user)
    }
}
