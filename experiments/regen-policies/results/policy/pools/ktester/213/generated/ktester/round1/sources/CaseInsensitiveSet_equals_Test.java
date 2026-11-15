package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_equals_Test {

    /**
     * A base set used as a fixture for the tests.
     */
    private Set<Object> baseSet;

    /**
     * A Set whose {@code size()} method always throws {@link IllegalStateException}.
     */
    private static class CustomSetSizeException extends AbstractSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new IllegalStateException("size() failure");
        }
    }

    /**
     * A Set whose iterator throws a {@link RuntimeException} when {@code next()} is called.
     */
    private static class CustomSetContainsException extends AbstractSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Object next() {
                    throw new RuntimeException("iterator.next() failure");
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test to avoid cross‑test contamination.
        baseSet = new CaseInsensitiveSet<>();
        baseSet.add("One");
        baseSet.add("Two");
        baseSet.add("Three");
        // non‑String element to verify mixed‑type handling
        baseSet.add(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the fixture – not strictly required because a new instance is created each time,
        // but it makes the intent explicit.
        baseSet.clear();
    }

    /**
     * Provides a stream of arguments for {@link #equalsBooleanScenarios}.
     *
     * Each entry contains:
     * <ul>
     *   <li>a description (used only for readability in the test name),</li>
     *   <li>a supplier that builds the {@code other} set,</li>
     *   <li>the expected boolean result.</li>
     * </ul>
     */
    private static Stream<Arguments> booleanScenarioProvider() {
        return Stream.of(// 1. Same instance – already covered, but kept for completeness
        Arguments.of("sameInstance", (Supplier<Set<?>>) () -> baseSet, true), // 2. Null argument
        Arguments.of("null", (Supplier<Set<?>>) () -> null, false), // 3. Non‑Set argument (String)
        Arguments.of("nonSetString", (Supplier<Set<?>>) () -> (Set<?>) (Object) "someString", false), // 4. Same elements, different case
        Arguments.of("sameElementsDifferentCase", (Supplier<Set<?>>) () -> {
            Set<String> s = new CaseInsensitiveSet<>();
            s.add("A");
            s.add("b");
            return s;
        }, true), // 5. Other set larger
        Arguments.of("otherLarger", (Supplier<Set<?>>) () -> {
            Set<String> s = new CaseInsensitiveSet<>();
            s.add("A");
            s.add("b");
            s.add("c");
            return s;
        }, false), // 6. Same size but missing element
        Arguments.of("sameSizeMissingElement", (Supplier<Set<?>>) () -> {
            Set<String> s = new CaseInsensitiveSet<>();
            s.add("A");
            s.add("c");
            return s;
        }, false), // 7. Both empty
        Arguments.of("bothEmpty", (Supplier<Set<?>>) () -> new HashSet<>(), true), // 8. Base non‑empty, other empty
        Arguments.of("baseNonEmptyOtherEmpty", (Supplier<Set<?>>) () -> new HashSet<>(), false), // 9. Base empty, other non‑empty
        Arguments.of("baseEmptyOtherNonEmpty", (Supplier<Set<?>>) () -> {
            Set<String> s = new CaseInsensitiveSet<>();
            s.add("x");
            return s;
        }, false), // 10. Same logical elements, different order / case
        Arguments.of("sameElementsDifferentOrderCase", (Supplier<Set<?>>) () -> {
            Set<String> s = new CaseInsensitiveSet<>();
            s.add("a");
            s.add("B");
            return s;
        }, true), // 11. Different concrete implementation (TreeSet)
        Arguments.of("treeSetImplementation", (Supplier<Set<?>>) () -> {
            Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            s.add("A");
            s.add("b");
            return s;
        }, true), // 12. Both contain null
        Arguments.of("bothContainNull", (Supplier<Set<?>>) () -> {
            Set<Object> s = new CaseInsensitiveSet<>();
            s.add(null);
            return s;
        }, true), // 13. Other contains null, base does not
        Arguments.of("otherContainsNullBaseDoesNot", (Supplier<Set<?>>) () -> {
            Set<Object> s = new CaseInsensitiveSet<>();
            s.add(null);
            return s;
        }, false), // 14. Other is a ConcurrentSkipListSet (same internal type)
        Arguments.of("concurrentSkipListSet", (Supplier<Set<?>>) () -> {
            Set<String> s = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
            s.add("A");
            s.add("b");
            return s;
        }, true));
    }

    @Test
    void equalsSameInstanceReturnsTrue() {
        assertTrue(baseSet.equals(baseSet), "A set must be equal to itself");
    }

    @Test
    void equalsDifferentTypeReturnsFalse() {
        assertFalse(baseSet.equals("not a set"), "A set should not be equal to an object of another type");
        assertFalse(baseSet.equals(null), "A set should not be equal to null");
    }

    @Test
    void equalsDifferentSizeReturnsFalse() {
        Set<Object> larger = new CaseInsensitiveSet<>(baseSet);
        larger.add("Extra");
        assertFalse(baseSet.equals(larger), "Sets with different sizes must not be equal");
        assertFalse(larger.equals(baseSet), "Equality must be symmetric");
    }

    @Test
    void equalsSameElementsDifferentCaseReturnsTrue() {
        Set<Object> other = new CaseInsensitiveSet<>();
        // different case
        other.add("ONE");
        // different case
        other.add("two");
        // different case
        other.add("ThReE");
        // same non‑String element
        other.add(42);
        assertTrue(baseSet.equals(other), "Case‑insensitive comparison should treat strings with different case as equal");
        assertTrue(other.equals(baseSet), "Equality must be symmetric");
    }

    @Test
    void equalsWhenContentsDifferReturnsFalse() {
        Set<Object> other = new CaseInsensitiveSet<>(baseSet);
        // remove one element
        other.remove("Two");
        // add a different element
        other.add("Four");
        assertFalse(baseSet.equals(other), "Sets with different elements must not be equal");
        assertFalse(other.equals(baseSet), "Equality must be symmetric");
    }

    @Test
    void equalsWithMixedTypesHandlesNonStringElementsCorrectly() {
        Set<Object> other = new CaseInsensitiveSet<>();
        other.add("One");
        other.add("Two");
        other.add("Three");
        // same integer
        other.add(42);
        // extra non‑String element
        other.add(100L);
        assertFalse(baseSet.equals(other), "Extra non‑String element makes the sets unequal");
        other.remove(100L);
        assertTrue(baseSet.equals(other), "After removing the extra element the sets should be equal");
    }

    @Test
    void equalsIsConsistentWithHashCodeContract() {
        Set<Object> other = new CaseInsensitiveSet<>(baseSet);
        assertTrue(baseSet.equals(other), "Identical sets must be equal");
        assertEquals(baseSet.hashCode(), other.hashCode(), "Equal sets must have the same hashCode");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("booleanScenarioProvider")
    void equalsBooleanScenarios(String description, Supplier<Set<?>> otherSupplier, boolean expected) {
        // Arrange
        Set<?> other = otherSupplier.get();
        // Act & Assert
        if (other == null) {
            assertEquals(expected, baseSet.equals(null), () -> "Scenario '" + description + "' expected " + expected);
        } else {
            assertEquals(expected, baseSet.equals(other), () -> "Scenario '" + description + "' expected " + expected);
        }
    }

    @Test
    void equalsPropagatesSizeException() {
        Set<Object> badSet = new CustomSetSizeException();
        // The exception should be thrown when equals() invokes size() on the other set.
        assertThrows(IllegalStateException.class, () -> baseSet.equals(badSet), "equals() must propagate IllegalStateException from other.size()");
    }

    @Test
    void equalsPropagatesContainsException() {
        Set<Object> badSet = new CustomSetContainsException();
        // The exception should be thrown during iteration inside containsAll().
        assertThrows(RuntimeException.class, () -> baseSet.equals(badSet), "equals() must propagate RuntimeException from other.iterator().next()");
    }

    /**
     * Test that a {@link ClassCastException} is propagated when the other set contains
     * an element of an incompatible type. {@link CaseInsensitiveSet} expects {@link String}
     * elements to be case‑insensitively compared, which typically involves a cast to {@code String}.
     */
    @Test
    void equalsPropagatesClassCastExceptionForIncompatibleElement() {
        // Base set is a CaseInsensitiveSet<String> (no non‑String elements)
        Set<String> stringSet = new CaseInsensitiveSet<>();
        stringSet.add("hello");
        // Other set contains an Integer; during containsAll, stringSet.contains(42) will cause
        // a ClassCastException inside the implementation (as per the contract description).
        Set<Integer> intSet = new HashSet<>();
        intSet.add(42);
        assertThrows(ClassCastException.class, () -> stringSet.equals(intSet), "equals() should propagate ClassCastException when encountering an incompatible element type");
    }
}
