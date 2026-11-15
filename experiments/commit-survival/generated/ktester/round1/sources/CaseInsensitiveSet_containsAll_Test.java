package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class CaseInsensitiveSet_containsAll_Test {

    private CaseInsensitiveSet<String> caseInsensitiveSet;

    /**
     * Minimal collection whose {@code iterator()} method returns {@code null}.
     * Used to verify that {@code containsAll} propagates a {@link NullPointerException}
     * when the enhanced‑for loop attempts to use a null iterator.
     */
    private static class NullIteratorCollection<E> implements Collection<E> {

        @Override
        public Iterator<E> iterator() {
            return null;
        }

        // The remaining methods are not used in the tests; they can throw
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Collection whose iterator throws a {@link RuntimeException} (here an
     * {@link IllegalStateException}) when {@code next()} is called.
     */
    private static class ExceptionThrowingIteratorCollection<E> implements Collection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new IllegalStateException("Intentional iterator failure");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        // Remaining methods are not exercised by the test
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        caseInsensitiveSet = new CaseInsensitiveSet<>();
        caseInsensitiveSet.add("Apple");
        caseInsensitiveSet.add("Banana");
        caseInsensitiveSet.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC and avoid cross‑test contamination
        caseInsensitiveSet.clear();
        caseInsensitiveSet = null;
    }

    static Stream<Collection<String>> basicAllPresentProvider() {
        return Stream.of(// exact match, same case
        Arrays.asList("Apple", "Banana", "Cherry"), // empty collection
        Collections.emptyList(), // duplicate elements
        Arrays.asList("Banana", "Banana", "Apple"));
    }

    static Stream<Collection<String>> missingElementProvider() {
        return Stream.of(Arrays.asList("Apple", "Banana", "Durian"), // set is cleared first to emulate "empty set" scenario
        Collections.singletonList("Durian"));
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        // given
        // different cases
        Collection<String> query = Arrays.asList("apple", "BANANA", "Cherry");
        // when
        boolean result = caseInsensitiveSet.containsAll(query);
        // then
        assertTrue(result, "containsAll should return true when all elements are present regardless of case");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        // given
        // "Durian" not in the set
        Collection<String> query = Arrays.asList("apple", "banana", "Durian");
        // when
        boolean result = caseInsensitiveSet.containsAll(query);
        // then
        assertFalse(result, "containsAll should return false when any element is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        // given
        Collection<String> empty = Collections.emptyList();
        // when
        boolean result = caseInsensitiveSet.containsAll(empty);
        // then
        assertTrue(result, "containsAll should return true for an empty collection");
    }

    @Test
    void containsAll_NonStringElements_MixedTypes() {
        // given a heterogeneous set
        CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
        mixedSet.add("Hello");
        mixedSet.add(42);
        mixedSet.add(true);
        // string case variation
        Collection<Object> query = Arrays.asList("hello", 42, true);
        // when
        boolean result = mixedSet.containsAll(query);
        // then
        assertTrue(result, "containsAll should correctly handle non‑String elements and case‑insensitive strings");
    }

    @Test
    void containsAll_NullElementInCollection_HandledAccordingToMapBehavior() {
        // given
        caseInsensitiveSet.add(null);
        Collection<Object> query = Arrays.asList(null, "apple");
        // when
        boolean result = caseInsensitiveSet.containsAll(query);
        // then
        // The underlying map permits null keys, so the method should return true
        assertTrue(result, "containsAll should return true when null is present in both the set and the collection");
    }

    @ParameterizedTest(name = "[{index}] collection = {0}")
    @MethodSource("basicAllPresentProvider")
    void containsAll_BasicAllPresent_ReturnsTrue(Collection<String> collection) {
        // when
        boolean result = caseInsensitiveSet.containsAll(collection);
        // then
        assertTrue(result, "containsAll should return true when all elements are present");
    }

    @Test
    void containsAll_MissingElement_EmptySet_ReturnsFalse() {
        // arrange: clear the set so it is empty
        caseInsensitiveSet.clear();
        Collection<String> collection = Collections.singletonList("Durian");
        // when
        boolean result = caseInsensitiveSet.containsAll(collection);
        // then
        assertFalse(result);
    }

    @Test
    void containsAll_NullElementInCollection_ThrowsNullPointerException() {
        // given: collection containing a null element
        Collection<Object> collection = Arrays.asList(null);
        // when / then
        assertThrows(NullPointerException.class, () -> caseInsensitiveSet.containsAll(collection), "containsAll should propagate NullPointerException for null keys");
    }

    @Test
    void containsAll_NullElementMixed_WithOtherElements_ThrowsNullPointerException() {
        Collection<Object> collection = Arrays.asList("Apple", null);
        assertThrows(NullPointerException.class, () -> caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_ArrayList_Success() {
        Collection<String> collection = new ArrayList<>(Arrays.asList("Apple", "Banana"));
        assertTrue(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_HashSet_Failure() {
        Collection<String> collection = new HashSet<>(Collections.singleton("Durian"));
        assertFalse(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_DifferentCase_Success() {
        Collection<String> collection = Arrays.asList("apple", "BANANA");
        assertTrue(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_SingleElementMixedCase_Success() {
        Collection<String> collection = Collections.singletonList("cHeRrY");
        assertTrue(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_NullCollectionArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> caseInsensitiveSet.containsAll(null));
    }

    @Test
    void containsAll_UnmodifiableList_Success() {
        Collection<String> collection = Collections.unmodifiableList(Arrays.asList("Apple", "Banana"));
        assertTrue(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_MixedTypes_NonStringElement_Failure() {
        // The set only contains Strings; an Integer should cause a false result
        Collection<Object> collection = Arrays.asList(42, "Apple");
        assertFalse(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_LargeCollection_AllPresent() {
        // Populate the set with 1000 items
        IntStream.rangeClosed(1, 1000).forEach(i -> caseInsensitiveSet.add("Item" + i));
        List<String> largeList = IntStream.rangeClosed(1, 1000).mapToObj(i -> "Item" + i).collect(Collectors.toList());
        assertTrue(caseInsensitiveSet.containsAll(largeList));
    }

    @Test
    void containsAll_TreeSet_Success() {
        SortedSet<String> treeSet = new TreeSet<>(Arrays.asList("Apple", "Banana"));
        assertTrue(caseInsensitiveSet.containsAll(treeSet));
    }

    @Test
    void containsAll_EarlyExit_FirstMissing_False() {
        Collection<String> collection = Arrays.asList("Durian", "Apple", "Banana", "Cherry");
        assertFalse(caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_InvalidElementType_Integer_ThrowsClassCastException() {
        Collection<Object> collection = Arrays.asList(123, "Apple");
        assertThrows(ClassCastException.class, () -> caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_InvalidElementType_Object_ThrowsClassCastException() {
        Collection<Object> collection = Collections.singletonList(new Object());
        assertThrows(ClassCastException.class, () -> caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_FaultyIterator_NullIterator_ThrowsNullPointerException() {
        Collection<Object> collection = new NullIteratorCollection<>();
        assertThrows(NullPointerException.class, () -> caseInsensitiveSet.containsAll(collection));
    }

    @Test
    void containsAll_FaultyIterator_ThrowsRuntimeException() {
        Collection<Object> collection = new ExceptionThrowingIteratorCollection<>();
        assertThrows(RuntimeException.class, () -> caseInsensitiveSet.containsAll(collection));
    }
}
