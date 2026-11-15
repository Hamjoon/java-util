package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_descendingIterator_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test (default natural ordering)
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Sets the private {@code internalSet} field of a {@link ConcurrentNavigableSetNullSafe}
     * instance via reflection.
     */
    private static void setInternalSet(ConcurrentNavigableSetNullSafe<?> target, NavigableSet<?> internal) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(target, internal);
    }

    private static Stream<Arguments> basicOrderingProvider() {
        return Stream.of(Arguments.of(List.of("a", "b", "c"), List.of("c", "b", "a")), Arguments.of(List.of("apple", "banana", "cherry"), List.of("cherry", "banana", "apple")));
    }

    @Test
    void descendingIteratorReturnsElementsInReverseOrderIncludingNull() {
        // Arrange
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // Act
        Iterator<String> descending = set.descendingIterator();
        // Assert – reverse of natural ordering (null is considered highest by the internal sentinel)
        assertEquals(null, descending.next(), "First element should be the null sentinel");
        assertEquals("cherry", descending.next());
        assertEquals("banana", descending.next());
        assertEquals("apple", descending.next());
        assertFalse(descending.hasNext());
    }

    @Test
    void descendingIteratorWorksWithCustomComparator() {
        // Arrange – comparator based on string length (null treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0
        assertTrue(set.add(null));
        // Ascending order would be: null, kiwi, apple, banana
        // Descending order should be the reverse:
        Iterator<String> descending = set.descendingIterator();
        assertEquals("banana", descending.next());
        assertEquals("apple", descending.next());
        assertEquals("kiwi", descending.next());
        assertEquals(null, descending.next());
        assertFalse(descending.hasNext());
    }

    @Test
    void descendingIteratorOnEmptySetHasNoElements() {
        // Act
        Iterator<String> descending = set.descendingIterator();
        // Assert
        assertFalse(descending.hasNext(), "Empty set should produce an iterator with no elements");
    }

    @Test
    void descendingIteratorRemoveReflectsInUnderlyingSet() {
        // Arrange
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add(null));
        Iterator<String> descending = set.descendingIterator();
        // The first element returned should be null
        assertEquals(null, descending.next());
        // Remove null via iterator
        descending.remove();
        // Verify removal is reflected in the set
        assertEquals(2, set.size());
        assertFalse(set.contains(null));
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        // Continue iteration and remove another element
        assertEquals("beta", descending.next());
        descending.remove();
        assertEquals(1, set.size());
        assertFalse(set.contains("beta"));
        assertTrue(set.contains("alpha"));
    }

    @Test
    void descendingIteratorBasicFunctionality() {
        // Arrange
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        // Act
        Iterator<String> it = set.descendingIterator();
        // Assert – reverse of natural order
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIteratorNextOnEmptySetThrowsNoSuchElementException() {
        Iterator<String> it = set.descendingIterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an exhausted iterator must throw NoSuchElementException");
    }

    @Test
    void descendingIteratorHandlesNullElement() {
        // Arrange – null is considered the highest element internally, thus appears first
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add(null));
        // Act
        Iterator<String> it = set.descendingIterator();
        // Assert – null first, then descending natural order of non‑null values
        assertNull(it.next(), "Null sentinel should be the first element in descending order");
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIteratorRemoveWorksCorrectly() {
        // Arrange
        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        assertTrue(set.add("z"));
        Iterator<String> it = set.descendingIterator();
        // First element is "z"
        assertEquals("z", it.next());
        // remove "z"
        it.remove();
        // Remaining elements should be "y", "x" in descending order
        assertEquals("y", it.next());
        // remove "y"
        it.remove();
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
        // Verify underlying set state
        assertEquals(1, set.size());
        assertTrue(set.contains("x"));
        assertFalse(set.contains("y"));
        assertFalse(set.contains("z"));
    }

    @Test
    void descendingIteratorRemoveBeforeNextThrowsIllegalStateException() {
        Iterator<String> it = set.descendingIterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void descendingIteratorDoubleRemoveThrowsIllegalStateException() {
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        Iterator<String> it = set.descendingIterator();
        assertEquals("b", it.next());
        // first valid remove
        it.remove();
        // Second remove without intervening next()
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() must throw IllegalStateException");
    }

    @Test
    void descendingIteratorSingleElementSet() {
        assertTrue(set.add("only"));
        Iterator<String> it = set.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("only", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIteratorWeaklyConsistentAfterConcurrentModification() {
        // Arrange
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        Iterator<String> it = set.descendingIterator();
        // Concurrent modification: add a new element after iterator creation
        set.add("c");
        // Act & Assert – iterator must not throw CME and must finish iteration
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // The iterator is weakly consistent: it may or may not contain "c".
        // We only assert that no exception was thrown and that the known elements are present.
        assertTrue(collected.contains("b"));
        assertTrue(collected.contains("a"));
        // If "c" appears, that's acceptable; otherwise it's also fine.
    }

    @Test
    void descendingIteratorWithNullInternalSetThrowsNPE() throws Exception {
        // Create instance via default constructor then inject null internalSet
        ConcurrentNavigableSetNullSafe<String> faulty = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(faulty, null);
        assertThrows(NullPointerException.class, faulty::descendingIterator, "Calling descendingIterator() when internalSet is null must throw NPE");
    }

    @Test
    void descendingIteratorUnsupportedRemovePropagatesException() throws Exception {
        // Underlying modifiable set
        NavigableSet<Object> modifiable = new ConcurrentSkipListSet<>();
        modifiable.add("a");
        modifiable.add("b");
        // Wrap with unmodifiable view
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(modifiable);
        // Inject unmodifiable set as internalSet
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(wrapper, unmodifiable);
        Iterator<String> it = wrapper.descendingIterator();
        assertEquals("b", it.next());
        // The underlying iterator does not support remove()
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() should propagate UnsupportedOperationException from underlying iterator");
    }

    @Test
    void descendingIteratorElementTypeMismatchThrowsClassCastException() throws Exception {
        // Create a normal instance and inject a mismatched element directly
        ConcurrentNavigableSetNullSafe<String> mismatched = new ConcurrentNavigableSetNullSafe<>();
        // Access internalSet via reflection
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> internal = (NavigableSet<Object>) f.get(mismatched);
        // Insert an Integer directly (bypassing generic type safety)
        internal.add(42);
        Iterator<String> it = mismatched.descendingIterator();
        // The cast to String inside unmaskNull should fail
        assertThrows(ClassCastException.class, it::next, "Calling next() when internalSet contains an element of wrong type must throw ClassCastException");
    }

    @ParameterizedTest
    @MethodSource("basicOrderingProvider")
    void descendingIteratorParameterizedBasicOrdering(List<String> input, List<String> expected) {
        input.forEach(set::add);
        Iterator<String> it = set.descendingIterator();
        List<String> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        assertEquals(expected, actual);
    }
}
