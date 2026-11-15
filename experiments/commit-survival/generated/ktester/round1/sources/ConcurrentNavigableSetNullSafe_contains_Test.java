package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_contains_Test {

    /**
     * Set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    /**
     * A comparator that orders strings by length – used for one of the scenarios.
     */
    private Comparator<String> lengthComparator;

    class NonComparable {
    }

    class NonComparable {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor gives a natural‑ordering set that is null‑safe.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Comparator used in a dedicated test.
        lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to help the GC and to guarantee isolation.
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// null is present → contains(null) true
        Arguments.of(true, true), // null absent → contains(null) false
        Arguments.of(false, false));
    }

    private static Stream<Arguments> elementPresenceProvider() {
        return Stream.of(// present
        Arguments.of(42, true), // absent
        Arguments.of(99, false));
    }

    @Test
    void containsShouldReturnTrueForExistingElementsIncludingNull() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        // Positive cases
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains(null));
        // Negative case
        assertFalse(set.contains("cherry"));
    }

    @Test
    void containsAfterRemovalShouldReflectCurrentState() {
        set.add("alpha");
        set.add("beta");
        set.add(null);
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains(null));
        // Remove an element and verify
        assertTrue(set.remove("alpha"));
        assertFalse(set.contains("alpha"));
        // Remove null and verify
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }

    @Test
    void containsWorksWhenSetIsCreatedFromACollection() {
        Collection<String> source = Arrays.asList("one", "two", null, "three");
        NavigableSet<String> fromCollection = new ConcurrentNavigableSetNullSafe<>(source);
        // The set should contain exactly the elements from the source collection.
        assertEquals(source.size(), fromCollection.size());
        for (String s : source) {
            assertTrue(fromCollection.contains(s), "Set should contain: " + s);
        }
        // An element not in the source collection must not be reported as present.
        assertFalse(fromCollection.contains("four"));
    }

    @Test
    void containsWithCustomComparatorShouldBehaveIdentically() {
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(customSet.add("kiwi"));
        // length 5
        assertTrue(customSet.add("apple"));
        // length 0
        assertTrue(customSet.add(null));
        // contains does not depend on ordering, only on element identity (including null)
        assertTrue(customSet.contains("kiwi"));
        assertTrue(customSet.contains("apple"));
        assertTrue(customSet.contains(null));
        assertFalse(customSet.contains("banana"));
    }

    @Test
    void containsShouldHandleObjectsOfDifferentTypeGracefully() {
        set.add("string");
        set.add(null);
        // A different type should simply return false rather than throwing ClassCastException.
        assertFalse(set.contains(123));
        assertFalse(set.contains(new Object()));
    }

    @Test
    void containsOnEmptySetShouldReturnFalseForAnyNonNullAndTrueForNullAfterAddition() {
        assertTrue(set.isEmpty());
        // Empty set does not contain anything.
        assertFalse(set.contains("anything"));
        assertFalse(set.contains(null));
        // After adding null, contains(null) becomes true.
        set.add(null);
        assertTrue(set.contains(null));
    }

    /**
     * ---------- Null handling (present vs absent) ----------
     */
    @ParameterizedTest(name = "[{index}] setContainsNull={0} → expected={1}")
    @MethodSource("nullHandlingProvider")
    void containsNullHandling(boolean setContainsNull, boolean expected) {
        // Use an Integer set to keep the test generic.
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        if (setContainsNull) {
            assertTrue(intSet.add(null), "Failed to add null to the set");
        }
        assertEquals(expected, intSet.contains(null), () -> "contains(null) should be " + expected + " when setContainsNull=" + setContainsNull);
    }

    /**
     * ---------- Element present / absent ----------
     */
    @ParameterizedTest(name = "[{index}] contains({0}) → expected={1}")
    @MethodSource("elementPresenceProvider")
    void containsElementPresence(Object element, boolean expected) {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate the set with a known element (42) only.
        assertTrue(intSet.add(42), "Failed to add 42 to the set");
        assertEquals(expected, intSet.contains(element), () -> "contains(" + element + ") should be " + expected);
    }

    /**
     * ---------- Empty set ----------
     */
    @Test
    void containsOnEmptySetReturnsFalseForArbitraryValue() {
        ConcurrentNavigableSetNullSafe<String> emptySet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(emptySet.isEmpty(), "Set should be empty at start");
        assertFalse(emptySet.contains("anyValue"), "Empty set must return false for any non‑null argument");
    }

    /**
     * ---------- Type mismatch – expect ClassCastException ----------
     */
    @Test
    void containsWithWrongArgumentTypeThrowsClassCastException() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // populate with a valid Integer
        intSet.add(1);
        // The cast to Integer fails before maskNull is invoked.
        assertThrows(ClassCastException.class, () -> intSet.contains("notAnInteger"));
    }

    /**
     * ---------- Subclass element (Number ← Double) ----------
     */
    @Test
    void containsAcceptsSubclassElements() {
        ConcurrentNavigableSetNullSafe<Number> numberSet = new ConcurrentNavigableSetNullSafe<>();
        Double value = 3.14;
        assertTrue(numberSet.add(value), "Failed to add Double value");
        // The Double is a subclass of Number, so the cast succeeds.
        assertTrue(numberSet.contains(value), "Set should contain the Double value");
    }

    /**
     * ---------- Equal but different instance (String) ----------
     */
    @Test
    void containsTreatsEqualDifferentInstancesAsPresent() {
        ConcurrentNavigableSetNullSafe<String> stringSet = new ConcurrentNavigableSetNullSafe<>();
        // distinct instance
        String stored = new String("test");
        assertTrue(stringSet.add(stored), "Failed to add stored string");
        // different instance, same contents
        String query = new String("test");
        assertTrue(stringSet.contains(query), "Set should consider equal strings (different instances) as present");
    }

    /**
     * ---------- Internal ordering exception (non‑Comparable argument) ----------
     */
    @Test
    void containsNonComparableArgumentThrowsClassCastException() {
        // Raw/Object set so that the generic cast succeeds.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe rawSet = new ConcurrentNavigableSetNullSafe();
        // Integer is comparable
        rawSet.add(1);
        // NonComparable does not implement Comparable, causing the internal set to fail.
        class NonComparable {
        }
        Object nonComp = new NonComparable();
        assertThrows(ClassCastException.class, () -> rawSet.contains(nonComp), "Calling contains() with a non‑Comparable object should throw ClassCastException");
    }

    /**
     * ---------- Null‑sentinel ordering exception ----------
     */
    @Test
    void containsNullWhenSentinelIsNotComparableThrowsClassCastException() {
        // Set is parametrised with Integer and does NOT contain a null element.
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // ensure there is at least one comparable element
        intSet.add(10);
        // The internal sentinel (a plain Object) cannot be compared with Integer,
        // so internalSet.contains(null) should raise ClassCastException.
        assertThrows(ClassCastException.class, () -> intSet.contains(null), "contains(null) should throw ClassCastException when sentinel is not comparable to existing elements");
    }
}
