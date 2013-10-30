package inc

class ItemRepository {
    Map<Item, Set<Item>> dependencies = new HashMap() //dependent (child) -> dependency (parent)
    Map<Item, String> fileAssignments = new HashMap()

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

    void addItem(Item item) {
        if (dependencies[item]) {
            return
        }
        dependencies[item] = new HashSet()
    }

    void addFileAssignment(Item item, File file) {
        fileAssignments[item] = file as String
    }

    void addDependency(Item parent, Item child) {
        addItem parent
        addItem child
        dependencies[child].add parent
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
