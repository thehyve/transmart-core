package org.transmartproject.search.indexing

import groovy.transform.Immutable
import groovy.transform.ToString

@Immutable
@ToString(includePackage = false)
class FacetsFieldImpl implements FacetsField, FacetsFieldDisplaySettings {
    String fieldName
    FacetsFieldType type

    int order
    String displayName
    boolean hideFromListings

    boolean isValid() {
        fieldName.endsWith(type.suffix) || predefined
    }

    private boolean isPredefined() {
        fieldName ==~ /[A-Z_-]+/
    }

    public static String getDefaultDisplayName(String name) {
        name
                .replaceFirst(/_.\z/, '')
                .replaceAll('_', ' ')
                .replaceAll(/(?:^| )./, { String t -> t.toUpperCase(Locale.ENGLISH) })
    }
    
    public static create(String fieldName,
                         boolean hideFromListings = false,
                         String displayName = null,
                         int order = FacetsFieldDisplaySettings.BROWSE_FIELDS_DEFAULT_PRECEDENCE,
                         FacetsFieldType fieldType = null) {
        fieldType = fieldType ?: FacetsFieldType.values().find { it.suffix == fieldName[-1] }
        assert fieldType != null: "Find field type for field $fieldName"

        if (displayName == null) {
            displayName = getDefaultDisplayName fieldName
        }

        new FacetsFieldImpl(fieldName, fieldType, order, displayName, hideFromListings)
    }

    @Override
    int compareTo(FacetsFieldDisplaySettings o) {
        this.order <=> o?.order ?: this.displayName <=> o?.displayName
    }
}
