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

class FileDependencies {

    Map<File, Set<File>> deps

    void makeFileDependencies(ItemRepository repository, String schema) {
        this.deps = new HashMap<File, Set<File>>()

        def fileForItem = { Item item ->
            def fileString =  repository.fileAssignments[item]
            if (!fileString) {
                Log.err "User $schema has a dependency list for $item, " +
                        "but no file assignment!"
                System.exit 1
            }
            File file = new File(fileString)
            if (!file.isFile()) {
                throw new RuntimeException("Could not find file for $file")
            }
            file
        }

        repository.dependencies.each { child, parents ->
            if (child.owner != schema || child.type == 'SYNONYM') {
                return
            }

            File childFile = fileForItem child

            if (!deps.containsKey(childFile)) {
                deps[childFile] = new HashSet<File>()
            }

            deps[childFile].addAll(parents.
                    findAll { it.owner == schema && it.type != 'SYNONYM' }.
                    inject([]) { List list, Item it ->
                        File file = fileForItem it
                        if (file == childFile) {
                            /* ignore file dependencies on itself */
                            return list
                        }
                        if (file.name == '_cross.sql') {
                            System.err "Item $child depends on $it, which is in _cross.sql!"
                            System.exit 1
                        }
                        list + file
                    })
        }
    }

    Set<File> getAt(File file) {
        deps[file]
    }

    Set<File> getChildrenFor(File file) {
        Set<File> result = new HashSet()
        for (entry in deps.entrySet()) {
            if (entry.value.contains(file)) {
                result.add entry.key
            }
        }
        result
    }

    private doTraverseDepthFirst(File file,
                                 Closure closure,
                                 Stack<File> stack,
                                 Set<File> seenFiles) {
        if (stack.contains(file)) {
            def message = "Circular dependency: $stack and back to $file"
            throw new RuntimeException(message)
        }

        stack << file
        this[file].each { dependencyFile ->
            doTraverseDepthFirst dependencyFile, closure, stack, seenFiles
        }
        stack.pop()

        if (seenFiles.contains(file)) {
            return
        }

        seenFiles << file

        closure file
    }

    void traverseDepthFirst(Closure closure) {
        def seenFiles = new HashSet()

        /* visits each file exactly once, parents first.
         * If the any closure call throws an exception, the traversal stops there
         */
        deps.keySet().each { File topFile ->
            doTraverseDepthFirst topFile, closure, new Stack(), seenFiles
        }
    }

    FileDependencies plus(FileDependencies other) {
        def ret = new FileDependencies()
        ret.deps = new LinkedHashMap(this.deps)
        ret.deps += other.deps

        ret
    }
}
