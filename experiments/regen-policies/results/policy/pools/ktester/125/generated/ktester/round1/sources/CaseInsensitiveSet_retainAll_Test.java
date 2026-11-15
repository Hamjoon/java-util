package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_retainAll_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private CaseInsensitiveSet<Object> set;

    /**
     * A reusable collection used as the argument to {@code retainAll}.
     */
    private Collection<Object> retainCollection;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a set with a mixture of String (different cases) and non‑String values.
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element
        set.add(42);
        // will be stored as "APPLE" but compared case‑insensitively
        set.add("APPLE");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector in long test suites.
        set = null;
        retainCollection = null;
    }

    /**
     * Supplies arguments for the parameterised {@link #retainAll_parameterised}.
     *
     * Each {@code Arguments} instance contains:
     * <ul>
     *   <li>a collection to pass to {@code retainAll}</li>
     *   <li>the expected boolean result (true if the set should change)</li>
     *   <li>a list representing the expected final contents of the set</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> retainAllProvider() {
        return Stream.of(// --- retainAll removes non‑matching elements (two variations) ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("apple", "banana"), true, Arrays.asList("apple", "banana")), org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("APPLE", "BANANA"), true, Arrays.asList("apple", "banana")), // --- retainAll returns false when collection contains all elements ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("apple", "banana", "cherry"), false, Arrays.asList("apple", "banana", "cherry")), // --- retainAll returns false when collection contains all elements case‑insensitively ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("APPLE", "BANANA", "CHERRY"), false, Arrays.asList("apple", "banana", "cherry")), // --- retainAll with empty collection clears the set ---
        org.junit.jupiter.params.provider.Arguments.of(Collections.emptyList(), true, Collections.emptyList()), // --- retainAll with collection that has no matching elements ---
        // According to the implementation this will clear the set.
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("durian", "elderberry"), true, Collections.emptyList()), // --- retainAll with collection containing null when set contains null ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList(null, "apple"), true, Arrays.asList(null, "apple")), // --- retainAll with collection containing duplicate elements ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("apple", "apple", "banana"), true, Arrays.asList("apple", "banana")), // --- retainAll with collection of unrelated type (integers) ---
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList(1, 2, 3), true, Collections.emptyList()));
    }

    @Test
    void retainAllRemovesElementsNotInArgument_andReturnsTrue() {
        // GIVEN – a collection that contains only a subset of the set's elements.
        // note lower‑case "one"
        retainCollection = Arrays.asList("one", "APPLE", 42);
        // WHEN
        boolean changed = set.retainAll(retainCollection);
        // THEN – the set should contain exactly the elements from the collection,
        //        respecting case‑insensitivity for strings, and the method must report a change.
        assertTrue(changed, "retainAll should return true when the set is modified");
        assertEquals(3, set.size(), "Set should contain three elements after retainAll");
        // case‑insensitive match
        assertTrue(set.contains("ONE"));
        // case‑insensitive match
        assertTrue(set.contains("apple"));
        assertTrue(set.contains(42));
        assertFalse(set.contains("Two"));
        assertFalse(set.contains("Three"));
    }

    @Test
    void retainAllWithSameElementsDifferentCase_returnsFalseAndLeavesSetUnchanged() {
        // GIVEN – a collection that contains all current elements, but with different string cases.
        retainCollection = Arrays.asList("ONE", "two", "tHrEe", 42, "apple");
        // Capture the original state for later comparison.
        Set<Object> originalSnapshot = new LinkedHashSet<>(set);
        // WHEN
        boolean changed = set.retainAll(retainCollection);
        // THEN – no modification should have occurred.
        assertFalse(changed, "retainAll should return false when the set is not modified");
        assertEquals(originalSnapshot, set, "Set content must remain unchanged");
    }

    @Test
    void retainAllWithEmptyCollection_clearsSet_andReturnsTrueIfNotAlreadyEmpty() {
        // GIVEN – an empty collection.
        retainCollection = Collections.emptyList();
        // WHEN
        boolean changed = set.retainAll(retainCollection);
        // THEN – the set must be empty and the method must report a change.
        assertTrue(changed, "retainAll should return true when a non‑empty set becomes empty");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAllWithNoMatchingElements_clearsSet_andReturnsTrue() {
        // GIVEN – a collection that shares no elements with the set (case‑insensitively).
        retainCollection = Arrays.asList("four", "five", "six");
        // WHEN
        boolean changed = set.retainAll(retainCollection);
        // THEN – the set becomes empty.
        assertTrue(changed, "retainAll should return true when all elements are removed");
        assertTrue(set.isEmpty(), "Set should be empty because nothing matched");
    }

    @Test
    void retainAllHandlesMixedTypesCorrectly() {
        // GIVEN – a collection that mixes strings (different case) and a non‑String element.
        retainCollection = Arrays.asList("tWo", "three", 42);
        // WHEN
        boolean changed = set.retainAll(retainCollection);
        // THEN – only the matching elements remain.
        assertTrue(changed, "retainAll should report a change");
        assertEquals(3, set.size());
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
        assertTrue(set.contains(42));
        assertFalse(set.contains("One"));
        assertFalse(set.contains("APPLE"));
    }

    @ParameterizedTest(name = "{index} => collection={0}, expectedChanged={1}")
    @MethodSource("retainAllProvider")
    void retainAll_parameterised(Collection<?> collection, boolean expectedChanged, List<Object> expectedElements) {
        // -----------------------------------------------------------------
        // Arrange – initialise the set to the baseline used in the JSON
        // -----------------------------------------------------------------
        set = new CaseInsensitiveSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Add a null element only for the specific test that expects it.
        if (expectedElements.contains(null)) {
            set.add(null);
        }
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        boolean changed = set.retainAll(collection);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertEquals(expectedChanged, changed, () -> "retainAll should return " + expectedChanged + " for collection " + collection);
        assertEquals(expectedElements.size(), set.size(), "Resulting set size does not match expectation");
        // Verify that each expected element is present (case‑insensitive for strings)
        for (Object expected : expectedElements) {
            assertTrue(set.contains(expected), () -> "Expected element not present after retainAll: " + expected);
        }
        // Ensure no unexpected elements remain
        for (Object actual : set) {
            assertTrue(expectedElements.contains(actual), () -> "Unexpected element present after retainAll: " + actual);
        }
    }

    @Test
    void retainAll_onAlreadyEmptySet_returnsFalse() {
        // Initialise an empty set explicitly.
        set = new CaseInsensitiveSet<>();
        retainCollection = Arrays.asList("apple");
        boolean changed = set.retainAll(retainCollection);
        assertFalse(changed, "retainAll on an empty set should return false");
        assertTrue(set.isEmpty(), "Set must remain empty");
    }

    @Test
    void retainAll_nullCollection_throwsNullPointerException() {
        retainCollection = null;
        assertThrows(NullPointerException.class, () -> set.retainAll(retainCollection), "Calling retainAll with a null collection must throw NPE");
    }
}
