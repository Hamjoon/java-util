package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CompactSet_iterator_Test {

    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet before each test.
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to aid garbage collection; not strictly necessary.
        set.clear();
    }

    @Test
    void iteratorShouldTraverseAllElements() {
        // given
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // when
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // then
        assertEquals(3, iterated.size(), "Iterator should return exactly three elements");
        assertTrue(iterated.containsAll(List.of("apple", "banana", "cherry")), "Iterator should contain all elements added to the set");
    }

    @Test
    void iteratorOnEmptySetShouldHaveNoElements() {
        // given – an empty set (created in @BeforeEach)
        // when
        Iterator<String> it = set.iterator();
        // then
        assertFalse(it.hasNext(), "Iterator of an empty set must report hasNext() == false");
    }

    @Test
    void iteratorRemoveShouldDeleteElementFromSet() {
        // given
        set.add("first");
        set.add("second");
        set.add("third");
        assertEquals(3, set.size());
        // when
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("second".equals(val)) {
                // remove the element returned by next()
                it.remove();
                break;
            }
        }
        // then
        assertEquals(2, set.size(), "Size should decrease after removal via iterator");
        assertFalse(set.contains("second"), "Removed element must no longer be present");
        assertTrue(set.contains("first"));
        assertTrue(set.contains("third"));
    }

    @Test
    void iteratorRemoveWithoutNextShouldThrowIllegalStateException() {
        // given
        set.add("only");
        // when
        Iterator<String> it = set.iterator();
        // then
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorRemoveTwiceWithoutNextShouldThrowIllegalStateException() {
        // given
        set.add("a");
        set.add("b");
        Iterator<String> it = set.iterator();
        // first element
        it.next();
        // valid removal
        it.remove();
        // when / then
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() twice in a row without an intervening next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("EmptySetIterator – next() throws NoSuchElementException")
    void emptySetIteratorNextThrows() {
        // given – an empty set (already created)
        Iterator<String> it = set.iterator();
        // then
        assertFalse(it.hasNext(), "hasNext() must be false for an empty set");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    @DisplayName("NonEmptySetIterator – preserves insertion order")
    void nonEmptySetIteratorPreservesOrder() {
        // given
        List<String> expected = List.of("alpha", "beta", "gamma");
        // insertion order
        expected.forEach(set::add);
        // when
        Iterator<String> it = set.iterator();
        List<String> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        // then
        assertEquals(expected, actual, "Iterator should return elements in the same order they were inserted");
    }

    @Test
    @DisplayName("IteratorConcurrentModification – throws CME after external add")
    void iteratorConcurrentModification() {
        // given
        set.add("x");
        set.add("y");
        Iterator<String> it = set.iterator();
        // external modification after iterator creation
        set.add("z");
        // when / then
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect structural modification and throw ConcurrentModificationException");
    }

    @Test
    @DisplayName("IteratorNoSuchElementAfterEnd – next() throws after last element")
    void iteratorNoSuchElementAfterEnd() {
        // given
        set.add("single");
        Iterator<String> it = set.iterator();
        // consume the only element
        assertTrue(it.hasNext());
        assertEquals("single", it.next());
        // now iterator is exhausted
        assertFalse(it.hasNext(), "hasNext() must be false after the last element is consumed");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after iterator is exhausted must throw NoSuchElementException");
    }

    @Test
    @DisplayName("IteratorHasNextIdempotent – repeated hasNext() calls are stable")
    void iteratorHasNextIdempotent() {
        // given
        set.add("a");
        set.add("b");
        Iterator<String> it = set.iterator();
        // before any next()
        assertTrue(it.hasNext());
        assertTrue(it.hasNext(), "Repeated hasNext() calls before next() must consistently return true");
        // first element
        assertEquals("a", it.next());
        // after first next()
        assertTrue(it.hasNext());
        assertTrue(it.hasNext(), "Repeated hasNext() calls after first next() must still return true");
        // second element
        assertEquals("b", it.next());
        // after consuming all elements
        assertFalse(it.hasNext());
        assertFalse(it.hasNext(), "Repeated hasNext() calls after exhaustion must consistently return false");
    }

    @Test
    @DisplayName("IteratorNullMap – iterator() throws NullPointerException when internal map is null")
    void iteratorNullMapThrowsNPE() throws Exception {
        // Construct a CompactSet via the public constructor
        CompactSet<String> corruptedSet = new CompactSet<>();
        // Use reflection to set the private final field 'map' to null
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(corruptedSet, null);
        // When calling iterator(), a NullPointerException is expected
        assertThrows(NullPointerException.class, corruptedSet::iterator, "Calling iterator() on a CompactSet with a null internal map must throw NullPointerException");
    }
}
