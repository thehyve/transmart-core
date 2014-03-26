package org.transmartproject.db.accesscontrol

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.transmartproject.core.users.Permission
import org.transmartproject.core.users.ProtectedOperation

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.*

class AccessLevel implements Permission {

    String name
    Long   value

    static hasMany = [searchAuthSecObjectAccesses: SecuredObjectAccess]

    static mapping = {
        table schema: 'searchapp', name: 'search_sec_access_level'

        id    column: 'search_sec_access_level_id', generator: 'assigned'
        name  column: 'access_level_name'
        value column: 'access_level_value'

        version false
    }

    static constraints = {
        name  nullable: true, maxSize: 200
        value nullable: true
    }

    /**
     * This is not in the database for now.
     */
    @Lazy
    static Multimap<String, ProtectedOperation> permissionToOperations = {
        def mapBuilder = ImmutableMultimap.builder()

        [API_READ, BUILD_COHORT, SHOW_SUMMARY_STATISTICS, RUN_ANALYSIS, EXPORT,
                SHOW_IN_TABLE].each {
            // don't iterate over WellKnownOperations.values() to avoid bugs in
            // the future
            mapBuilder.put 'OWN', it
            mapBuilder.put 'EXPORT', it
        }

        // all but api read, export and show in table to VIEW
        [BUILD_COHORT, SHOW_SUMMARY_STATISTICS, RUN_ANALYSIS].each {
            mapBuilder.put 'VIEW', it
        }

        mapBuilder.build()
    }()


    @Override
    public String toString() {
        com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .add("value", value).toString()
    }

    @Override
    boolean isCase(ProtectedOperation operation) {
        permissionToOperations.containsEntry(name, operation)
    }
}
