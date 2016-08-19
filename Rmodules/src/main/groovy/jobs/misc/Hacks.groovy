package jobs.misc

/**
 * Misc stuff that doesn't belong anywhere else and actually shouldn't exist.
 */
final class Hacks {
    private Hacks() {}

    /**
     * This method takes a conceptPath provided by the frontend and turns it into a String representation of
     * a concept key which the AssayConstraint can use. Such a string is pulled apart later in a
     * table_access.c_table_cd part and a concept_dimension.concept_path part.
     * The operation duplicates the first element of the conceptPath and prefixes it to the original with a double
     * backslash.
     * The assumption that the table code equals the first element of the full path breaks for across trial nodes
     * though, so we need special handling for that. The real solution, of course, is to have the front-end pass
     * concept keys instead.
     * @param conceptPath
     * @return String conceptKey
     */
    static String createConceptKeyFrom(String conceptPath) {
        if (conceptPath =~ '^\\\\Across Trials\\\\') {
            return "\\\\xtrials$conceptPath"
        }

        // This crazy dance with slashes is "expected behaviour"
        // as per http://groovy.codehaus.org/Strings+and+GString (search for Slashy Strings)
        def bs = '\\\\'
        "\\\\" + (conceptPath =~ /$bs([^$bs]+)$bs/)[0][-1] + conceptPath
    }
}
