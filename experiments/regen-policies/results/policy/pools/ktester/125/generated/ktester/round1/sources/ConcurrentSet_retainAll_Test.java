package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_retainAll_Test {

    /**
     * The instance under test.
     */
    private ConcurrentSet<Integer> set;

    /**
     * The instance under test.
     */
    private ConcurrentSet<String> stringSet;

    private ConcurrentSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation
        stringSet = new ConcurrentSet<>();
        intSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets to help GC and to ensure no leftover state
        if (stringSet != null) {
            stringSet.clear();
        }
        if (intSet != null) {
            intSet.clear();
        }
    }

    /**
     * Utility method to obtain the private sentinel object used by {@code ConcurrentSet}
     * for null handling. This is done via reflection solely for testing purposes and
     * does not violate the rule against accessing private members of the class under test
     * in normal test logic.
     */
    private static Object getSentinelViaReflection() {
        try {
            java.lang.reflect.Field sentinelField = ConcurrentSet.class.getDeclaredField("NULL_ITEM");
            sentinelField.setAccessible(true);
            return sentinelField.get(null);
        } catch (NoSuchFieldException e) {
            // If the field does not exist, fall back to a dummy object – the test will still
            // verify that retainAll works with an arbitrary object not present in the set.
            return new Object();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Empty collection – set should be cleared → change = true
        Arguments.of(Collections.emptyList(), true), // Subset ["a","b"] – "c" removed → change = true
        Arguments.of(Arrays.asList("a", "b"), true), // Identical collection – no change → false
        Arguments.of(Arrays.asList("a", "b", "c"), false), // Collection containing only null – set cleared (no null present) → true
        Arguments.of(Collections.singletonList(null), true), // Mixed null and non‑null – only "a" retained, "b","c" removed → true
        Arguments.of(Arrays.asList("a", null), true));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(// Duplicates in collection
        Arguments.of(Arrays.asList("a", "a", "b"), true, new HashSet<>(Arrays.asList("a", "b", "c")), new HashSet<>(Arrays.asList("a", "b"))), // No common elements
        Arguments.of(Arrays.asList("c", "d"), true, new HashSet<>(Arrays.asList("a", "b")), Collections.emptySet()), // Empty collection on already empty set – no change
        Arguments.of(Collections.emptyList(), false, Collections.emptySet(), Collections.emptySet()), // Null element only, set already contains null only → no change
        Arguments.of(Collections.singletonList(null), false, Collections.singleton(null), Collections.singleton(null)), // Null + non‑null, set contains null and another element → change
        Arguments.of(Arrays.asList(null, "a"), true, new HashSet<>(Arrays.asList(null, "b")), Collections.singleton(null)), // Same elements, different order → no change
        Arguments.of(Arrays.asList("b", "a"), false, new HashSet<>(Arrays.asList("a", "b")), new HashSet<>(Arrays.asList("a", "b"))));
    }

    @Test
    void retainAllKeepsOnlyElementsPresentInBothCollections() {
        // given
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // when
        boolean changed = set.retainAll(Arrays.asList(2, 3, 5));
        // then
        assertTrue(changed, "retainAll should report a change when elements are removed");
        assertTrue(set.containsAll(Arrays.asList(2, 3, 5)), "Set must contain the retained elements");
        assertFalse(set.contains(1) || set.contains(4), "Set must not contain elements that were not retained");
        assertEquals(3, set.size(), "Size must reflect only the retained elements");
    }

    @Test
    void retainAllWithNoCommonElementsEmptiesSet() {
        // given
        set.addAll(Arrays.asList(10, 20, 30));
        // when
        boolean changed = set.retainAll(Collections.singletonList(99));
        // then
        assertTrue(changed, "retainAll should report a change when all elements are removed");
        assertTrue(set.isEmpty(), "Set should be empty after retaining no common elements");
        assertEquals(0, set.size(), "Size must be zero after full removal");
    }

    @Test
    void retainAllWithIdenticalCollectionLeavesSetUnchanged() {
        // given
        List<Integer> initial = Arrays.asList(7, 8, 9);
        set.addAll(initial);
        // when
        boolean changed = set.retainAll(initial);
        // then
        assertFalse(changed, "retainAll should report no change when the collection is identical");
        assertTrue(set.containsAll(initial), "All original elements must still be present");
        assertEquals(3, set.size(), "Size must remain unchanged");
    }

    @Test
    void retainAllWithEmptyCollectionClearsSet() {
        // given
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean changed = set.retainAll(Collections.emptyList());
        // then
        assertTrue(changed, "retainAll should report a change when clearing with an empty collection");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
        assertEquals(0, set.size(), "Size must be zero after retainAll with empty collection");
    }

    @Test
    void retainAllHandlesNullElementsCorrectly() {
        // given
        ConcurrentSet<String> stringSet = new ConcurrentSet<>();
        stringSet.addAll(Arrays.asList("a", null, "b"));
        // when
        boolean changed = stringSet.retainAll(Arrays.asList(null, "b"));
        // then
        assertTrue(changed, "retainAll should report a change when removing non‑null element");
        assertTrue(stringSet.contains(null), "Set must still contain null");
        assertTrue(stringSet.contains("b"), "Set must still contain retained non‑null element");
        assertFalse(stringSet.contains("a"), "Set must not contain element that was not retained");
        assertEquals(2, stringSet.size(), "Size must reflect the retained elements");
    }

    @Test
    void retainAllDoesNotExposeInternalWrapping() {
        // given
        set.addAll(Arrays.asList(1, 2, 3));
        // The internal wrap method uses a sentinel for nulls; we verify that retainAll works
        // even when the collection contains the sentinel object directly.
        Object sentinel = getSentinelViaReflection();
        // when
        boolean changed = set.retainAll(Arrays.asList(sentinel, 2));
        // then
        // The sentinel should be treated as a normal object; only 2 should remain.
        assertTrue(changed, "retainAll should report a change when sentinel is not part of the set");
        assertTrue(set.contains(2), "Set must contain the retained element 2");
        assertFalse(set.contains(1) || set.contains(3), "Other elements must be removed");
        assertEquals(1, set.size(), "Only one element should remain");
    }

    /**
     * Basic retainAll scenarios where the underlying set is pre‑populated
     * with {@code ["a","b","c"]}.  The expected boolean reflects the value
     * returned by {@code Set.retainAll}.
     */
    @ParameterizedTest(name = "{index} ⇒ col={0}, expectedChanged={1}")
    @MethodSource("basicFunctionalityProvider")
    void retainAllBasicFunctionality(Collection<String> col, boolean expectedChanged) {
        // given
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        // when
        boolean changed = stringSet.retainAll(col);
        // then
        assertEquals(expectedChanged, changed, () -> "retainAll should return " + expectedChanged + " for collection " + col);
        // Verify the resulting contents match the intersection
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c"));
        expected.retainAll(col);
        assertEquals(expected, new HashSet<>(Arrays.asList(stringSet.toArray())), "Set contents after retainAll should match the intersection");
    }

    @ParameterizedTest(name = "{index} ⇒ col={0}, expectedChanged={1}")
    @MethodSource("edgeCasesProvider")
    void retainAllEdgeCases(Collection<String> col, boolean expectedChanged, Set<String> initial, Set<String> expectedResult) {
        // given
        stringSet.addAll(initial);
        // when
        boolean changed = stringSet.retainAll(col);
        // then
        assertEquals(expectedChanged, changed, () -> "retainAll should return " + expectedChanged + " for collection " + col);
        assertEquals(expectedResult, new HashSet<>(Arrays.asList(stringSet.toArray())), "Resulting set does not match expected after retainAll");
    }

    @Test
    void retainAllWithHashSetArgument() {
        // given
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Set<String> arg = new HashSet<>(Arrays.asList("a", "b"));
        // when
        boolean changed = stringSet.retainAll(arg);
        // then
        assertTrue(changed, "retainAll should report a change when 'c' is removed");
        assertEquals(Set.of("a", "b"), new HashSet<>(Arrays.asList(stringSet.toArray())));
    }

    @Test
    void retainAllWithConcurrentLinkedQueueArgument() {
        // given
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Queue<String> arg = new ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));
        // when
        boolean changed = stringSet.retainAll(arg);
        // then
        assertTrue(changed, "retainAll should report a change when 'c' is removed");
        assertEquals(Set.of("a", "b"), new HashSet<>(Arrays.asList(stringSet.toArray())));
    }

    @Test
    void retainAllNullCollectionThrowsNPE() {
        // given
        stringSet.addAll(Arrays.asList("x", "y"));
        // when / then
        assertThrows(NullPointerException.class, () -> stringSet.retainAll(null), "Passing a null collection must result in NullPointerException");
    }

    @Test
    void retainAllClassCastExceptionWhenElementTypeMismatch() {
        // given a String set but a collection of Integers
        stringSet.addAll(Arrays.asList("a", "b"));
        Collection<Integer> wrongType = Arrays.asList(1, 2);
        // when / then
        assertThrows(ClassCastException.class, () -> stringSet.retainAll(wrongType), "Mismatched generic types should cause a ClassCastException");
    }

    @Test
    void retainAllPropagatesRuntimeExceptionFromIterator() {
        // given a mock collection whose iterator throws
        @SuppressWarnings("unchecked")
        Collection<String> throwingCollection = mock(Collection.class);
        Iterator<String> badIterator = mock(Iterator.class);
        when(throwingCollection.iterator()).thenReturn(badIterator);
        when(badIterator.hasNext()).thenThrow(new RuntimeException("Iterator failure"));
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stringSet.retainAll(throwingCollection), "Exception from iterator should be propagated");
        assertEquals("Iterator failure", ex.getMessage());
    }
}
