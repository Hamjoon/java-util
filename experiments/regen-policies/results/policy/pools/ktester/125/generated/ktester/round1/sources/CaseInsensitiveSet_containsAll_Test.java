package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

// retained from template, though not used
@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_containsAll_Test {

    private CaseInsensitiveSet<Object> set;

    /**
     * Simple {@link Collection} implementation whose iterator throws a supplied
     * {@link RuntimeException} (or subclass) either from {@code hasNext()} or {@code next()}.
     */
    private static class CustomIterable implements Collection<Object> {

        private final RuntimeException toThrow;

        CustomIterable(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    // Throw on first call to hasNext()
                    throw toThrow;
                }

                @Override
                public Object next() {
                    throw toThrow;
                }
            };
        }

        // The remaining Collection methods are not used by containsAll and can be no‑ops.
        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(Object e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Object> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test case – default constructor uses a
        // case‑insensitive map that permits null keys.
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element
        set.add(42);
        // allow null handling
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        set = null;
    }

    private static Stream<Arguments> provideContainsAllCases() {
        return Stream.of(// empty collection → true
        Arguments.of(Collections.emptyList(), true, "empty collection should return true"), // all present, case‑insensitive
        Arguments.of(Arrays.asList("A", "b", "C"), true, "all elements present (case‑insensitive)"), // missing element → false
        Arguments.of(Arrays.asList("A", "missing", "C"), false, "collection contains a missing element"), // collection with null → expect NPE (backed by ConcurrentSkipListMap)
        Arguments.of(Arrays.asList((Object) null), NullPointerException.class, "null key not allowed by ConcurrentSkipListMap"), // integer not present → false
        Arguments.of(Arrays.asList(123), false, "integer not present in a String‑only set"), // large collection with last element missing → false
        Arguments.of(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"), false, "last element missing after many present"));
    }

    /**
     * Replaces the private {@code map} field of the given {@link CaseInsensitiveSet}
     * with a {@link ConcurrentSkipListMap}.  This map does not allow {@code null}
     * keys and therefore triggers a {@link NullPointerException} when {@code containsKey(null)}
     * is called.
     */
    private static void injectConcurrentSkipListMap(CaseInsensitiveSet<?> target) throws Exception {
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // Use a case‑insensitive comparator for String keys; other types are stored as‑is.
        ConcurrentSkipListMap<Object, Object> map = new ConcurrentSkipListMap<>(Comparator.comparing(Object::toString, String.CASE_INSENSITIVE_ORDER));
        // Populate the map with the current set contents to keep behaviour identical.
        for (Object e : target) {
            map.put(e, new Object());
        }
        mapField.set(target, map);
    }

    @Test
    void containsAll_AllStringsCaseInsensitive_ReturnsTrue() {
        // Given – a collection with the same strings in different cases
        Collection<Object> query = Arrays.asList("one", "TWO", "ThReE");
        // When
        boolean result = set.containsAll(query);
        // Then
        assertTrue(result, "containsAll should be true when all string elements match case‑insensitively");
    }

    @Test
    void containsAll_MixedTypesIncludingNonString_ReturnsTrue() {
        // Given – collection contains a non‑String element and a null that are present in the set
        Collection<Object> query = Arrays.asList(42, null, "ONE");
        // When
        boolean result = set.containsAll(query);
        // Then
        assertTrue(result, "containsAll should handle non‑String and null elements correctly");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        // Given – collection contains an element that is not in the set
        // "Four" missing
        Collection<Object> query = Arrays.asList("One", "Two", "Four");
        // When
        boolean result = set.containsAll(query);
        // Then
        assertFalse(result, "containsAll should return false when any element is absent");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        // Given – an empty collection
        Collection<Object> query = Collections.emptyList();
        // When
        boolean result = set.containsAll(query);
        // Then
        assertTrue(result, "containsAll must return true for an empty collection per Set contract");
    }

    @Test
    void containsAll_CollectionWithNullWhenSetLacksNull_ReturnsFalse() {
        // Given – a set that does NOT contain null (re‑initialise without null)
        set.remove(null);
        Collection<Object> query = Collections.singletonList(null);
        // When
        boolean result = set.containsAll(query);
        // Then
        assertFalse(result, "containsAll should return false when null is queried but not present");
    }

    @Test
    void containsAll_CollectionContainingDuplicateElements_ReturnsTrue() {
        // Given – collection with duplicate entries (duplicates should not affect the result)
        Collection<Object> query = Arrays.asList("one", "ONE", "One");
        // When
        boolean result = set.containsAll(query);
        // Then
        assertTrue(result, "containsAll should be true even if the collection contains duplicate elements");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideContainsAllCases")
    void containsAll_JsonDrivenTests(Collection<?> query, Object expected, String description) throws Exception {
        // Re‑initialise the set to contain only "a","b","c" (case‑insensitive) for this group
        set.clear();
        set.add("a");
        set.add("b");
        set.add("c");
        // For the null‑key test we need a backing map that rejects nulls.
        if (expected == NullPointerException.class) {
            injectConcurrentSkipListMap(set);
        }
        if (expected instanceof Class) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> set.containsAll(query), description);
        } else {
            boolean result = set.containsAll(query);
            assertEquals(expected, result, description);
        }
    }

    @Test
    void containsAll_EmptySetNonEmptyCollection_ReturnsFalse() {
        // Set is empty, collection has one element → false
        // make the set empty
        set.clear();
        Collection<Object> query = Collections.singletonList("a");
        assertFalse(set.containsAll(query), "empty set cannot contain any non‑empty collection");
    }

    @Test
    void containsAll_DuplicateElementsInCollection_ReturnsTrue() {
        // Set contains "a" and "b"
        set.clear();
        set.add("a");
        set.add("b");
        Collection<Object> query = Arrays.asList("a", "a", "b");
        assertTrue(set.containsAll(query), "duplicates in the collection must be ignored");
    }

    @Test
    void containsAll_NullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.containsAll(null), "passing a null collection should throw NPE");
    }

    @Test
    void containsAll_IncompatibleElement_ThrowsClassCastException() throws Exception {
        // The default backing map expects case‑insensitive strings; inserting a generic Object
        // should cause a ClassCastException when containsKey is invoked.
        set.clear();
        // ensure map is initialised with a String key
        set.add("a");
        Collection<Object> query = Collections.singletonList(new Object());
        assertThrows(ClassCastException.class, () -> set.containsAll(query), "incompatible element type should trigger ClassCastException");
    }

    @Test
    void containsAll_CollectionIsSelf_ReturnsTrue() {
        // The collection passed is the set itself – every element is trivially present.
        assertTrue(set.containsAll(set), "set.containsAll(set) must return true");
    }

    @Test
    void containsAll_IteratorThrowsRuntimeException_Propagates() {
        Collection<Object> bad = new CustomIterable(() -> {
            throw new RuntimeException("boom");
        });
        assertThrows(RuntimeException.class, () -> set.containsAll(bad), "RuntimeException from iterator should propagate");
    }

    @Test
    void containsAll_IteratorThrowsConcurrentModificationException_Propagates() {
        Collection<Object> bad = new CustomIterable(() -> {
            throw new ConcurrentModificationException("concurrent");
        });
        assertThrows(ConcurrentModificationException.class, () -> set.containsAll(bad), "ConcurrentModificationException should propagate");
    }

    @Test
    void containsAll_IteratorThrowsIllegalStateException_Propagates() {
        Collection<Object> bad = new CustomIterable(() -> {
            throw new IllegalStateException("illegal state");
        });
        assertThrows(IllegalStateException.class, () -> set.containsAll(bad), "IllegalStateException should propagate");
    }
}
