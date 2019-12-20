package org.transmartproject.db.multidimquery

import org.hibernate.ScrollableResults
import org.transmartproject.core.multidimquery.hypercube.Dimension.Density
import org.transmartproject.core.multidimquery.hypercube.Dimension.Packable
import org.transmartproject.core.multidimquery.hypercube.Dimension.Size
import org.transmartproject.db.i2b2data.ObservationFact
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.*

class HypercubeImplSpec extends Specification {

    def 'project map iterator'() {
        def data = [
                [1, 'a'] as Object[],
                [2, 'b'] as Object[]
        ]
        def testee = new ProjectionMapIterator(
                ['col1', 'col2'].withIndex().collectEntries(),
                [
                        next: { -> !data.empty },
                        get : { -> data.remove(0) }
                ] as ScrollableResults)

        when:
        def result = testee.iterator().collect()
        then:
        result == [
                [col1: 1, col2: 'a'],
                [col1: 2, col2: 'b'],
        ]
    }

    def 'modifier result iterator'() {
        def startDate = new Date()
        def data = [
                ['concept1', 'provider1', 'patient1', 'visit1', startDate, 1, '@', 'T', 'text value 1', null] as Object[],
                ['concept1', 'provider1', 'patient1', 'visit1', startDate, 1, 'md1', 'T', 'modifier 1 value 1', null] as Object[],
                ['concept1', 'provider1', 'patient1', 'visit1', startDate, 1, 'md2', 'N', 'E', 11] as Object[],
                ['concept1', 'provider1', 'patient1', 'visit1', startDate, 2, '@', 'T', 'text value 12', null] as Object[],
                ['concept1', 'provider1', 'patient1', 'visit1', startDate, 2, 'md2', 'N', 'E', 12] as Object[],
                ['concept1', 'provider1', 'patient2', 'visit1', startDate, 1, '@', 'T', 'text value 2', null] as Object[],
                ['concept1', 'provider1', 'patient2', 'visit1', startDate, 1, 'md1', 'T', 'modifier 1 value 2', null] as Object[],
                ['concept1', 'provider1', 'patient2', 'visit1', startDate, 1, 'md2', 'N', 'E', 20] as Object[],
        ]
        def testee = new HypercubeImpl.ModifierResultIterator(
                [
                        ModifierDimension.get('test_modifier_1', 'md1', ObservationFact.TYPE_TEXT, Size.SMALL,
                                Density.DENSE, Packable.NOT_PACKABLE, null, null),
                        ModifierDimension.get('test_modifier_2', 'md2', ObservationFact.TYPE_NUMBER, Size.SMALL,
                                Density.DENSE, Packable.NOT_PACKABLE, null, null)
                ],
                [
                        CONCEPT.alias,
                        PROVIDER.alias,
                        PATIENT.alias,
                        VISIT.alias,
                        START_TIME.alias,
                        'instanceNum',
                        'modifierCd',
                        'valueType',
                        'textValue',
                        'numberValue',
                ].withIndex().collectEntries(),
                [
                        next: { -> !data.empty },
                        get : { -> data.remove(0) }
                ] as ScrollableResults)

        when:
        def result = testee.iterator().collect()
        then:
        result.size() == 3
        result[0] == [
                (CONCEPT.alias)   : 'concept1',
                (PROVIDER.alias)  : 'provider1',
                (PATIENT.alias)   : 'patient1',
                (VISIT.alias)     : 'visit1',
                (START_TIME.alias): startDate,
                instanceNum       : 1,
                modifierCd        : '@',
                valueType         : 'T',
                textValue         : 'text value 1',
                numberValue       : null,
                test_modifier_1   : 'modifier 1 value 1',
                test_modifier_2   : 11
        ]
        result[1] == [
                (CONCEPT.alias)   : 'concept1',
                (PROVIDER.alias)  : 'provider1',
                (PATIENT.alias)   : 'patient1',
                (VISIT.alias)     : 'visit1',
                (START_TIME.alias): startDate,
                instanceNum       : 2,
                modifierCd        : '@',
                valueType         : 'T',
                textValue         : 'text value 12',
                numberValue       : null,
                test_modifier_2   : 12
        ]
        result[2] == [
                (CONCEPT.alias)   : 'concept1',
                (PROVIDER.alias)  : 'provider1',
                (PATIENT.alias)   : 'patient2',
                (VISIT.alias)     : 'visit1',
                (START_TIME.alias): startDate,
                instanceNum       : 1,
                modifierCd        : '@',
                valueType         : 'T',
                textValue         : 'text value 2',
                numberValue       : null,
                test_modifier_1   : 'modifier 1 value 2',
                test_modifier_2   : 20
        ]
    }
}
