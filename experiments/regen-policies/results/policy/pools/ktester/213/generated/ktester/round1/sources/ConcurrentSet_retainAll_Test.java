package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_retainAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to avoid cross‑test interference.
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the sets are cleared; helps the GC and keeps the test runner tidy.
        if (intSet != null)
            intSet.clear();
        if (stringSet != null)
            stringSet.clear();
    }

    /**
     * Supplies arguments for {@link #retainAll_edgeCases(String, Collection, Object)}.
     * The third element is either a {@link Boolean} (expected return value) or a
     * {@link Class} representing an expected exception.
     */
    private static Stream<Arguments> retainAllEdgeCaseProvider() {
        return Stream.of(// no change – all elements retained
        Arguments.of("no‑change – identical elements", Arrays.asList("a", "b"), false), // change – some elements removed
        Arguments.of("change – some elements removed", Collections.singletonList("a"), true), // change – all elements removed by empty collection
        Arguments.of("change – empty collection clears set", Collections.emptyList(), true), // no change – empty set retained by empty collection
        Arguments.of("no‑change – empty set & empty collection", Collections.emptyList(), false), // change – collection with no matching elements
        Arguments.of("change – no matching elements", Arrays.asList("x", "y"), true), // null collection → NPE
        Arguments.of("null collection throws NPE", null, NullPointerException.class), // collection containing only null (set has non‑null)
        Arguments.of("change – collection only null, set non‑null", Collections.singletonList(null), true), // no change – set contains null, collection contains null
        Arguments.of("no‑change – both contain null", Collections.singletonList(null), false), // change – set contains null, collection empty
        Arguments.of("change – empty collection removes null", Collections.emptyList(), true), // no change – collection is a superset of the set
        Arguments.of("no‑change – superset collection", Arrays.asList("a", "b", "c"), false), // change – collection with duplicate elements
        Arguments.of("change – duplicate elements in collection", Arrays.asList("a", "a"), true), // no change – collection is the same instance as the set
        Arguments.of("no‑change – collection is the set itself", // placeholder, will be replaced in test
        Collections.singleton("self"), false), // change – collection contains elements of different type
        Arguments.of("change – different element type", Arrays.asList(1, 2), true), // no change – empty set retained by non‑empty collection
        Arguments.of("no‑change – empty set retained by non‑empty collection", Collections.singletonList("x"), false));
    }

    @Test
    void retainAllRemovesUnwantedElements_andReturnsTrueWhenChanged() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // when
        boolean changed = intSet.retainAll(Arrays.asList(2, 3, 5));
        // then
        assertTrue(changed, "retainAll should return true when the set is modified");
        assertTrue(intSet.containsAll(Arrays.asList(2, 3, 5)), "Set should contain only the retained elements");
        assertFalse(intSet.contains(1) || intSet.contains(4), "Elements not present in the retain collection must be removed");
        assertEquals(3, intSet.size(), "Set size must reflect the retained elements");
    }

    @Test
    void retainAllWithNoMatchingElements_clearsSet_andReturnsTrue() {
        // given
        intSet.addAll(Arrays.asList(10, 20, 30));
        // when
        boolean changed = intSet.retainAll(Collections.singletonList(99));
        // then
        assertTrue(changed, "retainAll should return true when all elements are removed");
        assertTrue(intSet.isEmpty(), "Set must be empty after retaining a non‑existent element");
        assertEquals(0, intSet.size(), "Size must be zero after full clear");
    }

    @Test
    void retainAllWithIdenticalCollection_returnsFalseAndLeavesSetUnchanged() {
        // given
        List<Integer> initial = Arrays.asList(7, 8, 9);
        intSet.addAll(initial);
        // when
        boolean changed = intSet.retainAll(initial);
        // then
        assertFalse(changed, "retainAll should return false when the set is not modified");
        assertTrue(intSet.containsAll(initial), "All original elements must still be present");
        assertEquals(3, intSet.size(), "Size must remain unchanged");
    }

    @Test
    void retainAllWithEmptyCollection_clearsSet_andReturnsTrueIfNotAlreadyEmpty() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean changed = intSet.retainAll(Collections.emptyList());
        // then
        assertTrue(changed, "retainAll should return true when clearing a non‑empty set");
        assertTrue(intSet.isEmpty(), "Set must be empty after retaining an empty collection");
    }

    @Test
    void retainAllWithEmptyCollectionOnAlreadyEmptySet_returnsFalse() {
        // given: intSet is freshly created and therefore empty
        // when
        boolean changed = intSet.retainAll(Collections.emptyList());
        // then
        assertFalse(changed, "retainAll should return false when the set is already empty");
        assertTrue(intSet.isEmpty(), "Set should remain empty");
    }

    @Test
    void retainAllHandlesNullElementsCorrectly() {
        // given
        stringSet.addAll(Arrays.asList("a", null, "b", "c"));
        // when
        boolean changed = stringSet.retainAll(Arrays.asList(null, "b"));
        // then
        assertTrue(changed, "retainAll should report a change when null and 'b' are retained");
        assertEquals(2, stringSet.size(), "Set should contain exactly two elements after retainAll");
        assertTrue(stringSet.contains(null), "Set must still contain the null element");
        assertTrue(stringSet.contains("b"), "Set must still contain 'b'");
        assertFalse(stringSet.contains("a") || stringSet.contains("c"), "Elements not in the retain collection must be removed");
    }

    @Test
    void retainAllWithCollectionContainingDuplicates_behavesAsIfDuplicatesWereAbsent() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        // when: collection has duplicate values
        boolean changed = intSet.retainAll(Arrays.asList(2, 2, 3, 3));
        // then
        assertTrue(changed, "retainAll should return true when elements are removed");
        assertEquals(2, intSet.size(), "Only the distinct retained elements should remain");
        assertTrue(intSet.containsAll(Arrays.asList(2, 3)), "Set must contain 2 and 3");
    }

    /**
     * Parameterised test that executes the scenarios described in the JSON matrix.
     *
     * @param description short description of the scenario (used only for readability)
     * @param collection  collection supplied to {@code retainAll}; may be {@code null}
     * @param expected    either a {@link Boolean} (expected return value) or a {@link Class}
     *                    of an exception that should be thrown.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("retainAllEdgeCaseProvider")
    void retainAll_edgeCases(String description, Collection<?> collection, Object expected) {
        // --------------------------------------------------------------
        // Arrange – a set that initially contains "a" and "b"
        // --------------------------------------------------------------
        stringSet.addAll(Arrays.asList("a", "b"));
        // Special handling for the "self" case where the collection argument
        // should be the set itself.
        if ("no‑change – collection is the set itself".equals(description)) {
            // the set implements Collection
            collection = stringSet;
        }
        // --------------------------------------------------------------
        // Act / Assert
        // --------------------------------------------------------------
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception (e.g., NullPointerException)
            assertThrows((Class<? extends Throwable>) expected, () -> stringSet.retainAll((Collection<?>) collection), description);
        } else {
            // Expected a boolean result
            boolean result = stringSet.retainAll((Collection<?>) collection);
            assertEquals(expected, result, "Unexpected return value for scenario: " + description);
            // Verify the final state of the set matches the expectation.
            // The logic mirrors the description of each case.
            switch(description) {
                case "no‑change – identical elements":
                case "no‑change – empty set & empty collection":
                case "no‑change – superset collection":
                case "no‑change – both contain null":
                case "no‑change – collection is the set itself":
                case "no‑change – empty set retained by non‑empty collection":
                    // Set should stay unchanged.
                    assertEquals(2, stringSet.size(), "Set size should remain 2 for scenario: " + description);
                    assertTrue(stringSet.containsAll(Arrays.asList("a", "b")), "Set contents should stay unchanged for scenario: " + description);
                    break;
                case "change – some elements removed":
                    assertEquals(1, stringSet.size(), "Only one element should remain after retaining [\"a\"]");
                    assertTrue(stringSet.contains("a"));
                    break;
                case "change – empty collection clears set":
                case "change – no matching elements":
                case "change – empty collection removes null":
                case "change – different element type":
                    assertTrue(stringSet.isEmpty(), "Set should be empty after scenario: " + description);
                    break;
                case "change – collection only null, set non‑null":
                    // Original set had "a","b". After retaining only null, set becomes empty.
                    assertTrue(stringSet.isEmpty(), "Set should be empty because null does not match existing entries");
                    break;
                case "change – duplicate elements in collection":
                    // Retaining only "a" (duplicates ignored) removes "b".
                    assertEquals(1, stringSet.size(), "Only one element should remain after duplicate collection");
                    assertTrue(stringSet.contains("a"));
                    break;
                default:
                    // No additional verification needed for other cases.
                    break;
            }
        }
    }
}
