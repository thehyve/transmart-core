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

import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class DisjunctionDataConstraintFactorySpec extends Specification {

    StandardDataConstraintFactory testee = new StandardDataConstraintFactory()

    void testWithTwoSubConstraints() {
        String nameOfSubconstraint1 = 'subcontraint_1',
               nameOfSubconstraint2 = 'subcontraint_2';
        Map paramsOfSubconstraint = [param1: 'param1 value']
        DataConstraint fakeConstraint1 = Mock(DataConstraint),
                       fakeConstraint2 = Mock(DataConstraint)

        Closure createConstraint = Mock(Closure)

        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint1) >> fakeConstraint1
        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint2) >> fakeConstraint2

        when:
        def result = testee.createDisjunctionConstraint(subconstraints: [
                (nameOfSubconstraint1): paramsOfSubconstraint,
                (nameOfSubconstraint2): paramsOfSubconstraint
        ], createConstraint)

        then:
        result instanceof DisjunctionDataConstraint
        result.constraints.size() == 2
        fakeConstraint1 in result.constraints
        fakeConstraint2 in result.constraints
    }

    void testWithOneSubConstraint() {
        String nameOfSubconstraint = 'subcontraint_1'
        Map paramsOfSubconstraint = [param1: 'param1 value']
        DataConstraint fakeConstraint = Mock(DataConstraint)

        Closure createConstraint = Mock(Closure)

        createConstraint.call(paramsOfSubconstraint, nameOfSubconstraint) >> fakeConstraint

        when:
        def result = testee.createDisjunctionConstraint(subconstraints: [
                (nameOfSubconstraint): paramsOfSubconstraint
        ], createConstraint)

        then:
        result == fakeConstraint
    }

    void testWithZeroSubConstraints() {
        when:
        def result = testee.createDisjunctionConstraint(subconstraints: [:],
                { -> })

        then:
        result instanceof NoopDataConstraint
    }

    void testWithTwoSubConstraintsOfTheSameType() {
        String nameOfSubConstraints = 'subcontraint_1'
        List paramsOfSubConstraints = [
                [param1: 'param1 value 1'],
                [param1: 'param1 value 2'],
        ]
        DataConstraint fakeConstraint1 = Mock(DataConstraint),
                       fakeConstraint2 = Mock(DataConstraint)

        Closure createConstraint = Mock(Closure)

        createConstraint.call(paramsOfSubConstraints[0], nameOfSubConstraints) >> fakeConstraint1
        createConstraint.call(paramsOfSubConstraints[1], nameOfSubConstraints) >> fakeConstraint2

        when:
        def result = testee.createDisjunctionConstraint(subconstraints: [
                (nameOfSubConstraints): paramsOfSubConstraints
        ], createConstraint)

        then:
        result instanceof DisjunctionDataConstraint
        fakeConstraint1 in result.constraints
        fakeConstraint2 in result.constraints
    }

    void testWithBadSubConstraintDefinition() {
        when:
        testee.createDisjunctionConstraint(subconstraints: 'bad value',
                { -> })
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('to be of type interface java.util.Map')
    }

    void testFailureBuildingSubConstraint() {
        String nameOfSubconstraint = 'subcontraint_1'
        Map paramsOfSubconstraint = [param1: 'param1 value']
        Closure createConstraint = { a, b ->
            assert a == paramsOfSubconstraint
            assert b == nameOfSubconstraint
            throw new InvalidArgumentsException('foobar')
        }

        when:
        testee.createDisjunctionConstraint(
                subconstraints: [(nameOfSubconstraint): paramsOfSubconstraint],
                createConstraint)
        then:
        def e = thrown(InvalidArgumentsException)
        e.message == 'foobar'
    }

}
