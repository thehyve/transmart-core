package inc

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
                Log.err("There is already materialized view declaration for ${fcName}. Skip conflicting table creation.")
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

    Set<Item> getParents(Item item) {
        dependencies[item]
    }

    void writeSequential(Writer writer) {
        writeWithSorter { Item item, ignore ->
            writer.write "--\n-- Type: ${item.type}; Owner: ${item.owner}; Name: ${item.name}\n--\n"
            writer.write item.data
            writer.write "\n"
        }
    }

    void writeWithSorter(Closure sorter) {
        def seen = new HashSet()
        def stack = new Stack()
        for (entry in dependencies.entrySet()) {
            doWriteItem seen, stack, entry.key, entry.value, sorter
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
    }
}
