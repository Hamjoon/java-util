package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_equals_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableSet for each test and populate it with a known state.
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        set = null;
    }

    /**
     * Provides test arguments for the "Delegation to internal set" group.
     * Each element is an {@code Object[]} where:
     *   - index 0: the description (String)
     *   - index 1: the argument to pass to {@code equals}
     *   - index 2: the expected boolean result
     */
    private static Stream<Object[]> delegationArguments() {
        return Stream.of(new Object[] { "null argument – internal set empty", null, false }, new Object[] { "empty HashSet – internal set empty", new HashSet<>(), true }, new Object[] { "HashSet with 1,2,3 – internal set empty", new HashSet<>(Arrays.asList(1, 2, 3)), true }, new Object[] { "HashSet with 1,2 – internal set empty", new HashSet<>(Arrays.asList(1, 2)), false }, new Object[] { "different type (String) – internal set empty", "some string", false }, // placeholder, replaced in test
        new Object[] { "self reference – internal set empty", null, false });
    }

    /**
     * Provides test arguments for the "Additional scenarios" group.
     */
    private static Stream<Object[]> additionalArguments() {
        return Stream.of(new Object[] { "HashSet containing null – internal set contains null", new HashSet<>(Collections.singletonList((Object) null)), true }, new Object[] { "empty HashSet – internal set contains null", new HashSet<>(), false }, new Object[] { "empty TreeSet – internal set empty", new TreeSet<>(), true }, new Object[] { "TreeSet 1,2,3 – internal set contains 1,2,3", new TreeSet<>(Arrays.asList(3, 2, 1)), true }, new Object[] { "HashSet 1,2,3,4 – internal set contains 1,2,3", new HashSet<>(Arrays.asList(1, 2, 3, 4)), false }, new Object[] { "HashSet of Strings vs Integers – internal set contains Integers", new HashSet<>(Arrays.asList("1", "2", "3")), false }, new Object[] { "SealableSet wrapping same elements – internal set 1,2,3", new SealableSet<>(new HashSet<>(Arrays.asList(1, 2, 3))), true }, new Object[] { "SealableSet wrapping different elements – internal set 1,2,3", new SealableSet<>(new HashSet<>(Arrays.asList(4, 5, 6))), false }, new Object[] { "singleton set with 1 – internal set contains 1,2,3", Collections.singleton(1), false });
    }

    @Test
    void testEqualsReflexive() {
        set.add(10);
        set.add(20);
        set.add(null);
        assertEquals(set, set, "A set must be equal to itself (reflexivity).");
    }

    @Test
    void testEqualsSymmetric() {
        set.add(10);
        set.add(20);
        set.add(null);
        SealableSet<Integer> other = new SealableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(null);
        assertTrue(set.equals(other), "set should equal other with same contents");
        assertTrue(other.equals(set), "symmetry: other should equal set");
    }

    @Test
    void testEqualsWithSameContent() {
        SealableSet<Integer> other = new SealableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(null);
        assertEquals(set, other, "Sets with identical elements must be equal.");
    }

    @Test
    void testEqualsWithDifferentContent() {
        SealableSet<Integer> other = new SealableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(null);
        // extra element
        other.add(30);
        assertNotEquals(set, other, "Sets with different elements must not be equal.");
    }

    @Test
    void testEqualsWithDifferentType() {
        // Comparing to a standard java.util.Set should return false because the underlying
        // implementation is a different class, even if the contents match.
        assertNotEquals(set, java.util.Collections.emptySet(), "SealabeSet should not be equal to an object of a different type.");
    }

    @Test
    void testEqualsWithNull() {
        assertNotEquals(set, null, "Any non‑null object must not be equal to null.");
    }

    @Test
    void testEqualsTransitive() {
        SealableSet<Integer> a = new SealableSet<>(sealedSupplier);
        SealableSet<Integer> b = new SealableSet<>(sealedSupplier);
        SealableSet<Integer> c = new SealableSet<>(sealedSupplier);
        a.add(10);
        a.add(20);
        a.add(null);
        b.add(10);
        b.add(20);
        b.add(null);
        c.add(10);
        c.add(20);
        c.add(null);
        // a == b and b == c implies a == c
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("delegationArguments")
    void testEqualsDelegationToInternalSet(String description, Object argument, boolean expected) {
        // Arrange – the internal set is empty for most cases; for the self‑reference case we
        // need to use the actual SealableSet instance.
        if ("self reference – internal set empty".equals(description)) {
            // compare with itself
            argument = set;
        }
        // Act
        boolean result = set.equals(argument);
        // Assert
        assertEquals(expected, result, () -> String.format("equals(%s) should return %s", argument, expected));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalArguments")
    void testEqualsAdditionalScenarios(String description, Object argument, boolean expected) {
        // Populate the internal set with a known collection for the relevant cases.
        // The description strings give us a hint about the required internal state.
        if (description.contains("contains null")) {
            set.add(null);
        } else if (description.contains("contains 1,2,3")) {
            set.addAll(Arrays.asList(1, 2, 3));
        } else if (description.contains("internal set empty")) {
            // leave it empty
        }
        // Act
        boolean result = set.equals(argument);
        // Assert
        assertEquals(expected, result, () -> String.format("equals(%s) should return %s", argument, expected));
    }
}
