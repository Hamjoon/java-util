package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollFirst_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑order) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to aid GC; not strictly necessary but keeps the pattern tidy.
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> pollFirstBehaviorProvider() {
        return Stream.of(// Empty set
        Arguments.of(Collections.emptyList(), null, "When the set is empty, pollFirst() returns null"), // Only a null element
        Arguments.of(Collections.singletonList(null), null, "When the set contains only a null element, pollFirst() returns null"), // Single non‑null element
        Arguments.of(Collections.singletonList("alpha"), "alpha", "When the set contains a single non‑null element \"alpha\""), // Multiple non‑null elements – natural ordering
        Arguments.of(Arrays.asList("beta", "gamma", "delta"), "beta", "When the set contains multiple non‑null elements, pollFirst() returns the smallest (\"beta\")"), // Null together with other elements – null is lowest
        Arguments.of(Arrays.asList(null, "epsilon", "zeta"), null, "When the set contains null and other non‑null elements, pollFirst() returns null"));
    }

    @Test
    void pollFirstOnEmptySetReturnsNull() {
        // GIVEN an empty set (created in @BeforeEach)
        assertTrue(set.isEmpty(), "Pre‑condition: set should be empty");
        // WHEN pollFirst is invoked
        String result = set.pollFirst();
        // THEN the result is null and the set remains empty
        assertNull(result, "pollFirst on an empty set must return null");
        assertTrue(set.isEmpty(), "Set should still be empty after pollFirst");
    }

    @Test
    void pollFirstReturnsNullWhenNullElementIsPresent() {
        // GIVEN a set containing a null element and other strings
        assertTrue(set.add(null), "Adding null should succeed");
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        // Verify the set contains all three elements
        assertEquals(3, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        // WHEN pollFirst is called
        String first = set.pollFirst();
        // THEN null is returned (null is the smallest element in natural order)
        assertNull(first, "pollFirst should return the null element first");
        // AND the set now contains only the non‑null elements
        assertEquals(2, set.size());
        assertFalse(set.contains(null));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
    }

    @Test
    void pollFirstReturnsElementsInNaturalOrderAfterNullIsRemoved() {
        // GIVEN a set with null and several ordered strings
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // WHEN we repeatedly pollFirst
        assertNull(set.pollFirst(), "First poll should return null");
        assertEquals("apple", set.pollFirst(), "Second poll should return 'apple'");
        assertEquals("banana", set.pollFirst(), "Third poll should return 'banana'");
        assertEquals("cherry", set.pollFirst(), "Fourth poll should return 'cherry'");
        // THEN the set is empty
        assertTrue(set.isEmpty(), "Set should be empty after polling all elements");
    }

    @Test
    void pollFirstWithCustomComparatorOrdersByLengthIncludingNull() {
        // GIVEN a set that orders strings by their length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(set.add(null), "Add null");
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // WHEN pollFirst is called repeatedly
        assertNull(set.pollFirst(), "First poll should return null (length 0)");
        assertEquals("kiwi", set.pollFirst(), "Second poll should return 'kiwi' (length 4)");
        assertEquals("apple", set.pollFirst(), "Third poll should return 'apple' (length 5)");
        assertEquals("banana", set.pollFirst(), "Fourth poll should return 'banana' (length 6)");
        // THEN the set is empty
        assertTrue(set.isEmpty(), "Set should be empty after all elements are polled");
    }

    @Test
    void pollFirstDoesNotThrowWhenSetContainsOnlyNull() {
        // GIVEN a set that only contains null
        assertTrue(set.add(null));
        assertEquals(1, set.size());
        // WHEN pollFirst is invoked
        String result = set.pollFirst();
        // THEN it returns null and the set becomes empty
        assertNull(result);
        assertTrue(set.isEmpty());
    }

    /**
     * Covers the JSON‑driven behavior group “pollFirst() behavior”.
     * Each argument supplies the initial elements, the expected result of the first pollFirst(),
     * and a short description used as the test name.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("pollFirstBehaviorProvider")
    void pollFirstBehavior(List<String> initialElements, String expectedResult, String description) {
        // Populate the set with the supplied elements (null values are allowed)
        for (String e : initialElements) {
            set.add(e);
        }
        // Perform the operation under test
        String actual = set.pollFirst();
        // Verify the returned value matches expectation
        assertEquals(expectedResult, actual, "pollFirst returned unexpected value");
        // Verify that the returned element has been removed from the set
        if (expectedResult != null) {
            assertFalse(set.contains(expectedResult), "Returned element should no longer be present");
        } else {
            // expectedResult == null means the null sentinel should have been removed
            assertFalse(set.contains(null), "Null element should have been removed");
        }
    }

    /**
     * Covers the JSON‑driven group “pollFirst() removal semantics”.
     * The set starts with three ordered strings; we repeatedly call pollFirst()
     * and verify the sequence of returned values, ending with a null after the set is empty.
     */
    @Test
    void pollFirstRemovalSemantics() {
        // GIVEN a set with three ordered strings
        set.add("a");
        set.add("b");
        set.add("c");
        // WHEN/THEN we poll sequentially and check the returned values
        assertEquals("a", set.pollFirst(), "First poll should return \"a\"");
        assertEquals("b", set.pollFirst(), "Second poll should return \"b\"");
        assertEquals("c", set.pollFirst(), "Third poll should return \"c\"");
        assertNull(set.pollFirst(), "Fourth poll (empty set) should return null");
    }

    /**
     * Tests the exceptional scenario where the internal navigable set has been set to {@code null}
     * (simulating a corrupted internal state). The method should throw a {@link NullPointerException}
     * when it attempts to delegate to {@code internalSet.pollFirst()}.
     */
    @Test
    void pollFirstThrowsWhenInternalSetIsNull() throws Exception {
        // Create a fresh instance using the default constructor
        ConcurrentNavigableSetNullSafe<String> brokenInstance = new ConcurrentNavigableSetNullSafe<>();
        // Use reflection to replace the private 'internalSet' field with null
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(brokenInstance, null);
        // Verify that invoking pollFirst now results in NPE
        assertThrows(NullPointerException.class, brokenInstance::pollFirst, "Calling pollFirst on a set with a null internalSet should throw NullPointerException");
    }
}
