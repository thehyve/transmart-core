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

package inc.oracle

class ItemRepository {
    Map<Item, Set<Item>> dependencies = new HashMap() //dependent (child) -> dependency (parent)
    Map<Item, String> fileAssignments = new HashMap()
    private Map<String, Item> itemsNamesMap = new HashMap()

    /**
     * Remove dependency and file assignment information for objects
     * owned by an user different from the one passed in.
     *
     * The resulting ItemRepository is different in nature in that the
     * "dependencies" map (adjacency list) will have no entries for objects
     * for other schemas' objects, though they will appear in the values of
     * the map.
     *
     * @param user
     * @return
     */
    ItemRepository forUser(String user) {
        user = user.toUpperCase(Locale.ENGLISH)

        def ret = new ItemRepository()
        ret.dependencies = dependencies.findAll { it.key.owner == user }
        ret.fileAssignments = fileAssignments.findAll { it.key.owner == user }
        ret
    }

    Item addItem(Item item) {
        if (dependencies[item]) {
            return item
        }

        String fcName = "${item.owner}.${item.name}"
        if(itemsNamesMap.containsKey(fcName)) {
            def oldItem = itemsNamesMap[fcName]
            /**
             * Oracle materialized views appears as both tables and materialized views at the same time.
             * Below is way to resolve the name conflict.
             */
            if(item.type == 'TABLE' && oldItem.type == 'MATERIALIZED_VIEW') {
                Log.warn("There is already materialized view declaration for ${fcName}. Skip conflicting table creation.")
                return oldItem
            }
            if(item.type == 'MATERIALIZED_VIEW' && oldItem.type == 'TABLE') {
                Log.err("There is already table declaration for ${fcName}. Replacing it in favor of the materialized view.")
                dependencies[item] = dependencies[oldItem]
                dependencies[item].remove(item)
                itemsNamesMap[fcName] = item
                return item
            }
        }

        dependencies[item] = new HashSet()
        itemsNamesMap[fcName] = item
        item
    }

    void addFileAssignment(Item item, File file) {
        fileAssignments[item] = file as String
    }

    void addDependency(Item parent, Item child) {
        def _child = addItem(child)
        def _parent = addItem(parent)
        if(_child != _parent) {
            dependencies[_child].add _parent
        }
    }

    Set<Item> getChildren(Item item) {
        Set<Item> result = new HashSet()
        for (entry in dependencies.entrySet()) {
            if (entry.value.contains(item)) {
                result.add entry.key
            }
        }
        result
    }

    Map<Item /* child */, Set<Item> /* parents */> getCrossDependencies() {
        def result = dependencies.findAll { Item child, Set<Item> parents ->
            parents.any { parent ->
                child.owner != parent.owner &&
                        child.type != 'SYNONYM'
            }
        }.collectEntries { Item child, Set<Item> parents ->
            [
                    child,
                    parents.findAll { parent -> child.owner != parent.owner }
            ]
        }

        result
    }

    Set<Item> getParents(Item item) {
        dependencies[item]
    }

    void writeSequential(Writer writer) {
        writeWithSorter { Item item, ignore ->
            writeItem(item, writer)
        }
    }

    static void writeItem(Item item, Writer writer) {
        writer.write "--\n-- Type: ${item.type}; Owner: ${item.owner}; Name: ${item.name}\n--\n"
        writer.write item.data
        writer.write "\n"
    }

    void writeWithSorter(Closure<Void> doWithItem) {
        def seen = new HashSet()
        def stack = new Stack()
        for (entry in dependencies.entrySet()) {
            doWriteItem seen, stack, entry.key, entry.value, doWithItem
        }
    }

    void doWriteItem(Set<Item> seen, Stack<Item> stack, Item item, Set<Item> itemDependencies, Closure doWithItem) {
        if (seen.contains(item)) {
            return
        }
        if (stack.contains(item)) {
            Log.err "Circular dependency for $item: $stack (and back to $item)"
        }
        stack << item
        seen << item

        for (depItem in itemDependencies) {
            doWriteItem seen, stack, depItem, dependencies[depItem], doWithItem
        }

        doWithItem item, this
        stack.pop()
    }

    ItemRepository plus(ItemRepository other) {
        def result = new ItemRepository()

        result.dependencies = new HashMap(this.dependencies)
        other.dependencies.each { Item child, Set<Item> parents ->
            if (!result.dependencies[child]) {
                result.dependencies[child] = new HashSet()
            }
            result.dependencies[child] += parents
        }

        result.fileAssignments = this.fileAssignments + other.fileAssignments
        result
    }

    void addGrantsForCrossDependencies() {
        crossDependencies.each { Item child, Set<Item> crossParents ->
            crossParents.each { parent -> addGrantForCrossDependency(child, parent) }
        }
    }

    private void addGrantForCrossDependency(Item childItem, Item parentItem) {
        assert childItem.owner != parentItem.owner

        def grantItem = grantItemForItem(parentItem, childItem.owner)
        if (grantItem) {
            addDependency parentItem, grantItem
            addDependency grantItem, childItem
        }
    }

    private GrantItem grantItemForItem(Item item, String grantee) {
        if (item.type == 'TABLE' || item.type == 'VIEW' || item.type == 'MATERIALIZED_VIEW' || item.type == 'SEQUENCE' ) {
            new GrantItem(item, 'SELECT', grantee)
        }
    }
}
