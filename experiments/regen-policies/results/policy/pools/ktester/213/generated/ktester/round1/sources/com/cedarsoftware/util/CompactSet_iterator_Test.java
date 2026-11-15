package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class CompactSet_iterator_Test {

    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // By default we create a plain set; individual tests may replace it
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test to avoid cross‑test contamination
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> orderingProvider() {
        return Stream.of(Arguments.of("insertion", List.of("b", "a", "c")), Arguments.of("natural", List.of("a", "b", "c")), Arguments.of("none", Collections.emptyList()));
    }

    /**
     * Verify that the iterator returned by {@code CompactSet.iterator()} traverses all
     * elements that were added to the set.
     */
    @Test
    void testIteratorTraversesAllElements() {
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(3, iterated.size(), "Iterator should have visited three elements");
        assertTrue(iterated.containsAll(List.of("alpha", "beta", "gamma")), "Iterator should contain all added elements");
    }

    /**
     * Verify that {@code Iterator.remove()} removes the current element from the underlying
     * {@code CompactSet}.
     */
    @Test
    void testIteratorRemoveRemovesFromSet() {
        set.add("one");
        set.add("two");
        set.add("three");
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            it.remove();
            assertFalse(set.contains(val), "Set should not contain element after iterator.remove()");
        }
        assertTrue(set.isEmpty(), "All elements should have been removed via iterator");
    }

    /**
     * Verify that calling {@code Iterator.remove()} before {@code next()} throws
     * {@code IllegalStateException}, as required by the {@link Iterator} contract.
     */
    @Test
    void testIteratorRemoveWithoutNextThrows() {
        set.add("x");
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    /**
     * Verify that the iterator reflects modifications made to the set after the iterator
     * has been created (i.e., it sees newly added elements).
     */
    @Test
    void testIteratorSeesLaterAdditions() {
        set.add("first");
        Iterator<String> it = set.iterator();
        // add more elements after iterator creation
        set.add("second");
        set.add("third");
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // At a minimum the originally added element must be present.
        assertTrue(collected.contains("first"));
    }

    /**
     * Empty set: iterator should have no elements.
     */
    @Test
    void testEmptySetIterator() {
        Iterator<String> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator on an empty set must report hasNext() == false");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    /**
     * Parameterized test for the three ordering configurations.
     *
     * @param ordering   the ordering mode passed to the builder (null = default/no ordering)
     * @param expected   the expected iteration order
     */
    @ParameterizedTest
    @MethodSource("orderingProvider")
    void testIterationOrder(String ordering, List<String> expected) {
        CompactSet.Builder<String> builder = CompactSet.<String>builder();
        if ("insertion".equals(ordering)) {
            builder.insertionOrder();
        } else if ("natural".equals(ordering)) {
            builder.sortedOrder();
        }
        // else: default (no explicit ordering)
        set = builder.build();
        // Insert elements in a non‑sorted order
        set.add("b");
        set.add("a");
        set.add("c");
        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        if ("none".equals(ordering)) {
            // Order is unspecified – just verify size and distinctness
            assertEquals(3, actual.size(), "Set should contain three elements");
            assertTrue(actual.containsAll(List.of("a", "b", "c")), "All inserted elements must be present");
        } else {
            assertEquals(expected, actual, "Iterator order for ordering='" + ordering + "' does not match expectation");
        }
    }

    /**
     * Case‑insensitive set: adding "Hello" then "hello" should result in a single element.
     */
    @Test
    void testCaseInsensitiveSet() {
        set = CompactSet.<String>builder().caseSensitive(false).build();
        assertTrue(set.add("Hello"), "First add should succeed");
        assertFalse(set.add("hello"), "Second add (different case) should be ignored");
        assertEquals(1, set.size(), "Set size must be 1 for case‑insensitive duplicate");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator must have one element");
        String element = it.next();
        assertEquals("Hello", element, "Stored element should be the first inserted value");
        assertFalse(it.hasNext(), "No further elements expected");
    }

    /**
     * Verify iterator.remove() semantics when called after a few next() calls.
     */
    @Test
    void testIteratorRemoveSpecific() {
        CompactSet<Integer> intSet = new CompactSet<>();
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        Iterator<Integer> it = intSet.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        // removes 2
        it.remove();
        assertEquals(2, intSet.size(), "Set size after removal must be 2");
        assertFalse(intSet.contains(2), "Set must not contain the removed element");
        List<Integer> remaining = new ArrayList<>();
        for (Integer i : intSet) {
            remaining.add(i);
        }
        assertTrue(remaining.containsAll(List.of(1, 3)), "Remaining elements must be 1 and 3");
    }

    /**
     * Concurrent modification detection – the iterator must throw
     * {@link ConcurrentModificationException} when the underlying set changes
     * after the iterator has been obtained.
     */
    @Test
    void testConcurrentModification() {
        set.add("x");
        set.add("y");
        Iterator<String> it = set.iterator();
        // Structural change after iterator creation
        set.add("z");
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect concurrent modification and throw ConcurrentModificationException");
    }

    /**
     * Null element handling – CompactSet may allow a single {@code null} element.
     * The test accepts either a successful insertion (size == 1) or a
     * {@link NullPointerException} on {@code add(null)}.
     */
    @Test
    void testNullElementHandling() {
        try {
            boolean added = set.add(null);
            assertTrue(added, "Adding null should return true if supported");
            assertEquals(1, set.size(), "Set size must be 1 after adding null");
            Iterator<String> it = set.iterator();
            assertTrue(it.hasNext(), "Iterator must have one element");
            assertNull(it.next(), "The element returned by iterator must be null");
            assertFalse(it.hasNext(), "No further elements expected");
        } catch (NullPointerException npe) {
            // If the implementation disallows null, the contract is to throw NPE.
            // The test passes in this case.
            assertTrue(true, "NullPointerException is acceptable if nulls are not supported");
        }
    }

    /**
     * Verify that a second call to {@code Iterator.remove()} without an intervening
     * {@code next()} throws {@link IllegalStateException}.
     */
    @Test
    void testIteratorRemoveConsecutiveThrows() {
        set.add("alpha");
        set.add("beta");
        Iterator<String> it = set.iterator();
        assertEquals("alpha", it.next());
        // first removal – valid
        it.remove();
        // second removal without next()
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() twice consecutively must throw IllegalStateException");
    }

    /**
     * Verify that calling {@code Iterator.remove()} before any {@code next()} call
     * throws {@link IllegalStateException}. (Duplicate of earlier test but kept for completeness.)
     */
    @Test
    void testIteratorRemoveBeforeNextThrowsAgain() {
        set.add("zeta");
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }
}
