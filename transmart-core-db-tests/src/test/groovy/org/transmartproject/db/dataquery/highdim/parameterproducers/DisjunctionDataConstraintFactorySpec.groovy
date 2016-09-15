/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
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
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.parameterproducers

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.gmock.WithGMock
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint

import static org.hamcrest.Matchers.*

@WithGMock
@Integration
@Rollback
@Slf4j
class DisjunctionDataConstraintFactorySpec extends Specification {

    StandardDataConstraintFactory testee = new StandardDataConstraintFactory()

    void testWithTwoSubConstraints() {
        String         nameOfSubconstraint1  = 'subcontraint_1',
                       nameOfSubconstraint2  = 'subcontraint_2';
        Map            paramsOfSubconstraint = [ param1: 'param1 value' ]
        DataConstraint fakeConstraint1 = mock(DataConstraint),
                       fakeConstraint2 = mock(DataConstraint)

        Closure createConstraint = mock(Closure)

        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint1).
                returns(fakeConstraint1)
        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint2).
                returns(fakeConstraint2)

        play {
            def result = testee.createDisjunctionConstraint(subconstraints: [
                    (nameOfSubconstraint1): paramsOfSubconstraint,
                    (nameOfSubconstraint2): paramsOfSubconstraint
            ], createConstraint)

            expect: result allOf(
                    isA(DisjunctionDataConstraint),
                    hasProperty('constraints', contains(
                            is(sameInstance(fakeConstraint1)),
                            is(sameInstance(fakeConstraint2)),
                    ))
            )
        }
    }

    void testWithOneSubConstraint() {
        String         nameOfSubconstraint  = 'subcontraint_1'
        Map            paramsOfSubconstraint = [ param1: 'param1 value' ]
        DataConstraint fakeConstraint = mock(DataConstraint)

        Closure createConstraint = mock(Closure)

        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint).
                returns(fakeConstraint)

        play {
            def result = testee.createDisjunctionConstraint(subconstraints: [
                    (nameOfSubconstraint): paramsOfSubconstraint
            ], createConstraint)

            expect: result is(sameInstance(fakeConstraint))
        }
    }

    void testWithZeroSubConstraints() {
        def result = testee.createDisjunctionConstraint(subconstraints: [:],
                { -> })

        expect: result is(instanceOf(NoopDataConstraint))
    }

    void testWithTwoSubConstraintsOfTheSameType() {
        String nameOfSubConstraints  = 'subcontraint_1'
        List   paramsOfSubConstraints = [
                [ param1: 'param1 value 1' ],
                [ param1: 'param1 value 2' ],
        ]
        DataConstraint fakeConstraint1 = mock(DataConstraint),
                       fakeConstraint2 = mock(DataConstraint)

        Closure createConstraint = mock(Closure)

        createConstraint.call(paramsOfSubConstraints[0], nameOfSubConstraints).
                returns(fakeConstraint1)
        createConstraint.call(paramsOfSubConstraints[1], nameOfSubConstraints).
                returns(fakeConstraint2)

        play {
            def result = testee.createDisjunctionConstraint(subconstraints: [
                    (nameOfSubConstraints): paramsOfSubConstraints
            ], createConstraint)

            expect: result allOf(
                    isA(DisjunctionDataConstraint),
                    hasProperty('constraints', contains(
                            sameInstance(fakeConstraint1),
                            sameInstance(fakeConstraint2),
                    ))
            )
        }
    }

    void testWithBadSubConstraintDefinition() {
        def message = shouldFail InvalidArgumentsException, {
            testee.createDisjunctionConstraint(subconstraints: 'bad value',
                    { -> })
        }

        expect: message containsString('to be of type interface java.util.Map')
    }

    void testFailureBuildingSubConstraint() {
        String  nameOfSubconstraint  = 'subcontraint_1'
        Map     paramsOfSubconstraint = [ param1: 'param1 value' ]
        Closure createConstraint = mock(Closure)

        createConstraint.call(is(paramsOfSubconstraint), is(nameOfSubconstraint)).
                raises(InvalidArgumentsException, 'foobar')

        play {
            def message = shouldFail InvalidArgumentsException, {
                testee.createDisjunctionConstraint(
                        subconstraints: [(nameOfSubconstraint): paramsOfSubconstraint],
                        createConstraint)
            }
            expect: message is('foobar')
        }
    }
}
