package org.transmartproject.batch.i2b2.mapping

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.batch.item.validator.ValidationException
import org.transmartproject.batch.i2b2.variable.ConceptI2b2Variable
import org.transmartproject.batch.i2b2.variable.ModifierI2b2Variable

/**
 * Takes a set of fact variables (concepts and modifiers) and groups them.
 *
 * Validation provided.
 *
 * TODO: on the complex side; would benefit from needs some unit testing
 */
@TypeChecked
@Slf4j
class FileFactEntriesSchema implements Map<I2b2MappingEntry, List<I2b2MappingEntry>> {

    final String filename

    @Delegate
    final Map<I2b2MappingEntry /* concept */, List<I2b2MappingEntry> /* mods */> conceptModifierEntryMap

    private final IdentityHashMap<ConceptI2b2Variable, I2b2MappingEntry> conceptVarToEntryMap

    private FileFactEntriesSchema(String filename,
                                  Map<I2b2MappingEntry, List<I2b2MappingEntry>> map,
                                  IdentityHashMap<ConceptI2b2Variable, I2b2MappingEntry> conceptEntryMapping) {
        def builder = ImmutableMap
                .<I2b2MappingEntry, ImmutableList<I2b2MappingEntry>> builder()
        map.each { I2b2MappingEntry conceptEntry,
                   List<I2b2MappingEntry> modifierEntries ->
            builder.put(conceptEntry, ImmutableList.copyOf(modifierEntries))
        }

        conceptModifierEntryMap = builder.build() as Map<I2b2MappingEntry, List>
        this.conceptVarToEntryMap = conceptEntryMapping //as IdentityHashMap<ConceptI2b2Variable, I2b2MappingEntry>
        this.filename = filename
    }

    // suppress needed because of Groovy bug when replacing the collect with *.filename
    @SuppressWarnings('UnnecessaryCollectCall')
    static FileFactEntriesSchema buildFor(Collection<I2b2MappingEntry> allEntriesForFile) {
        Map<I2b2MappingEntry, List<I2b2MappingEntry>> entries = [:]
        Map<ConceptI2b2Variable, I2b2MappingEntry> conceptToEntryMap =
                new IdentityHashMap()

        // spread operator is broken with static compile here
        assert allEntriesForFile.collect { I2b2MappingEntry entry -> entry.filename }.unique().size() == 1

        allEntriesForFile.findAll { I2b2MappingEntry entry ->
            entry.i2b2Variable instanceof ConceptI2b2Variable
        }.each { I2b2MappingEntry entry ->
            conceptToEntryMap[(ConceptI2b2Variable) entry.i2b2Variable] = entry
            entries[entry] = []
        }

        allEntriesForFile.findAll { I2b2MappingEntry entry ->
            entry.i2b2Variable instanceof ModifierI2b2Variable
        }.each { I2b2MappingEntry e ->
            I2b2MappingEntry entry = conceptToEntryMap.get(
                    ((ModifierI2b2Variable) e.i2b2Variable).boundConceptVariable)
            assert entries[entry] != null
            entries[entry] << e
        }

        new FileFactEntriesSchema(allEntriesForFile.find().filename,
                entries, conceptToEntryMap)
    }

    void validate() {
        validateRepeatedConceptCodes()
        validateRepeatedModifiers()
    }

    private void validateRepeatedModifiers() {
        conceptModifierEntryMap.each { I2b2MappingEntry conceptEntry,
                                       List<I2b2MappingEntry> modifierEntries ->
            Set<String> repeatedModifiers = modifierEntries.countBy {
                I2b2MappingEntry modifierEntry ->
                    ((ModifierI2b2Variable) modifierEntry.i2b2Variable).modifierCode
            }.findAll { String modifierCode, Integer count ->
                count > 1
            }.keySet()

            if (repeatedModifiers) {
                throw new ValidationException("The modifier codes " +
                        "$repeatedModifiers are repeated for concept entry " +
                        "$conceptEntry. This is not allowed. If you need " +
                        "multiple instances of the same modifier for the " +
                        "same concept, repeat first the concept entry in " +
                        "order to start another fact group.")
            }
        }
    }

    private void validateRepeatedConceptCodes() {
        // concepts showing up more than once
        Set<String> concepts = findRepeatedConceptCodes()

        // that are associated to the some column more than once
        Map<String, List<Integer>> codeRepColList = concepts.collectEntries { String it ->
            [it, findRepeatedColumnAssociationsForConcept(it)]
        }.findAll { conceptCode, repeatedInColumnsList ->
            !((List) repeatedInColumnsList).empty
        }
        // key is mapped multiple times to to each of the columns in the values

        // this is permitted if the modifiers are always distinct
        // or at least associated to different columns
        // So use as key for uniqueness the set of (modifier variable, column number)
        codeRepColList.each { String code, List<Integer> repeatedForCols ->
            repeatedForCols.each { int col ->
                Map<Set<Tuple>, Integer> counts
                counts = getModifierEntryListListsForConceptCodeAndColumn(code, col).countBy {
                    List<I2b2MappingEntry> modifierEntries ->
                        modifierEntries.collect { I2b2MappingEntry entry ->
                            new Tuple((ModifierI2b2Variable) entry.i2b2Variable,
                                    entry.columnNumber)
                        } as Set
                }

                // case where the modifiers are the same and mapped to the same columns
                counts
                        .findAll { Set<Tuple> tuples, Integer c -> c > 1 }
                        .each { Set<Tuple> tuples, Integer c ->
                    throw new ValidationException("The concept code $code is read multiple times " +
                            "from the column $col and, for this combination, a set of " +
                            "(modifier, column from which the modifier is read) is repeated " +
                            "(this set being $tuples)")
                }

                // the more benign case
                LoggerFactory.getLogger(FileFactEntriesSchema).info(
                        "The concept code $code is read multiple times from " +
                                "column $col. This is allowed because the set of " +
                                "(modifier, column from which of the modifier is " +
                                "read) is different in each case; just make sure " +
                                "it's intentional.")
            }
        }
    }

    private Set<String> findRepeatedConceptCodes() {
        conceptVarToEntryMap.keySet()
                .countBy { ConceptI2b2Variable it -> it.conceptCode }
                .findAll { concept, count ->
            count > 1
        }.keySet()
    }

    // column can be repeated
    private List<Integer> getColumnAssociationsForConcept(String conceptCode) {
        getConceptEntriesForConceptCode(conceptCode)*.columnNumber
    }

    private List<Integer> findRepeatedColumnAssociationsForConcept(String conceptCode) {
        getColumnAssociationsForConcept(conceptCode)
                .countBy(Closure.IDENTITY)
                .findAll { column, count -> count > 1 }
                .keySet() as List<Integer>
    }

    private List<I2b2MappingEntry> getConceptEntriesForConceptCode(String code) {
        // findAll uses map internally. Don't use
        def result = []
        conceptVarToEntryMap.each { ConceptI2b2Variable var, I2b2MappingEntry entry ->
            if (var.conceptCode == code) {
                result << entry
            }
        }
        result
    }

    private List<List<I2b2MappingEntry>> getModifierEntryListListsForConceptCodeAndColumn(String code, int column) {
        getConceptEntriesForConceptCode(code).findAll { I2b2MappingEntry entry ->
            entry.columnNumber == column
        }.collect { I2b2MappingEntry entry ->
            owner[entry]
        }
    }
}
