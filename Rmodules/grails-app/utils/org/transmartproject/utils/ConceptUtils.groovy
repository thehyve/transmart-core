package org.transmartproject.utils

import java.util.regex.Pattern

class ConceptUtils {

    static final String SEP = '\\'

    private static shortenToUniqueTails(List<List<String>> partsList, int step = 0) {
        int revPos = -(step + 1)
        Map<String, List<List<String>>> groups = partsList.groupBy { List<String> parts ->
            if (parts.size() > step) {
                parts[revPos]
            }
        }
        groups.each {
            //Concepts that grouped under it.key == null do not have so many levels.
            if (it.key && it.value.size() > 1) {
                shortenToUniqueTails(it.value, step + 1)
            } else if (it.key) {
                def parts = it.value[0]
                //remove all parts of the concept path from the root down to current element (excluding it).
                //note that we are changing input list here
                (parts.size() + revPos).times { parts.remove(0) }
            }
        }
    }

    /**
     * Shorten the paths to shortest ones, but still unique in current scope
     * (among concepts represented in the input list) by removing higher levels
     * that are not important for uniqueness of this concept in given scope.
     * Although it does not guarantee uniqueness if input list already contains repetitions.
     * e.g. Given input list ['\A\B\C\', '\A\2\C\', '\B\C\', '\B\C\']
     * function returns ['\A\B\C\', '\2\C\', '\B\C\', '\B\C\']
     * @param conceptPathes concepts to shorten
     * @return shortened and normalized concept paths
     */
    static List<String> shortestUniqueTails(List<String> conceptPathes) {
        List<List<String>> conceptsParts = conceptPathes.collect { it.split(Pattern.quote(SEP)).findAll() }
        shortenToUniqueTails(conceptsParts)
        conceptsParts.collect { SEP + it.join(SEP) + SEP }
    }

}
