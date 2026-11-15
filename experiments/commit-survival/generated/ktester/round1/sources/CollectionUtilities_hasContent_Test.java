package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_hasContent_Test {

    /**
     * Provides a stream of arguments representing each test case from the JSON
     * specification.  Each argument consists of:
     * <ul>
     *   <li>the collection to test (may be {@code null})</li>
     *   <li>the expected boolean result</li>
     *   <li>a textual description used as the assertion message</li>
     * </ul>
     */
    private static Stream<Arguments> hasContentProvider() {
        // 1. Null collection
        Collection<?> nullCol = null;
        // 2. Empty ArrayList
        Collection<?> emptyArrayList = new ArrayList<>();
        // 3. Non‑empty List via Arrays.asList
        Collection<Integer> list123 = Arrays.asList(1, 2, 3);
        // 4. Non‑empty LinkedHashSet with a single element
        Collection<String> linkedHashSetA = new LinkedHashSet<>(Collections.singleton("a"));
        // 5. Immutable empty List from Collections.emptyList()
        Collection<?> emptyList = Collections.emptyList();
        // 6. Immutable empty Set from Collections.emptySet()
        Collection<?> emptySet = Collections.emptySet();
        // 7. List that contains a single null element
        Collection<Object> listWithSingleNull = Arrays.asList((Object) null);
        // 8. Non‑empty LinkedList
        Collection<String> linkedListXYZ = new LinkedList<>(Arrays.asList("x", "y", "z"));
        // 9. Non‑empty HashSet
        Collection<Integer> hashSetNumbers = new HashSet<>(Arrays.asList(10, 20, 30));
        // 10. Single‑element ArrayList (different from the empty case)
        Collection<String> singleElementArrayList = new ArrayList<>(Collections.singletonList("only"));
        // 11. Custom AbstractCollection with elements (size 2)
        Collection<Integer> customNonEmpty = new AbstractCollection<Integer>() {

            private final List<Integer> data = Arrays.asList(1, 2);

            @Override
            public Iterator<Integer> iterator() {
                return data.iterator();
            }

            @Override
            public int size() {
                return data.size();
            }
        };
        // 12. Custom AbstractCollection that reports size 0 and provides an empty iterator
        Collection<Object> customEmpty = new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }
        };
        return Stream.of(Arguments.of(nullCol, false, "When the collection is null, hasContent should return false."), Arguments.of(emptyArrayList, false, "When the collection is an empty ArrayList, hasContent should return false."), Arguments.of(list123, true, "When the collection is a non‑empty List, hasContent should return true."), Arguments.of(linkedHashSetA, true, "When the collection is a non‑empty Set, hasContent should return true."), Arguments.of(emptyList, false, "An immutable empty List returned by Collections.emptyList() should be considered empty."), Arguments.of(emptySet, false, "An immutable empty Set returned by Collections.emptySet() should be considered empty."), Arguments.of(listWithSingleNull, true, "A collection that contains a single null element is still non‑empty; hasContent should return true."), Arguments.of(linkedListXYZ, true, "A non‑empty LinkedList (which implements List) should be recognized as having content."), Arguments.of(hashSetNumbers, true, "A non‑empty HashSet should be recognized as having content."), Arguments.of(singleElementArrayList, true, "A single‑element ArrayList (different from the empty case) should return true."), Arguments.of(customNonEmpty, true, "A custom AbstractCollection implementation with elements should be treated as having content."), Arguments.of(customEmpty, false, "A custom AbstractCollection that reports size 0 and provides an empty iterator should be considered empty."));
    }

    @Test
    void hasContent_NullCollection_ReturnsFalse() {
        // given
        List<Object> nullList = null;
        // when
        boolean result = CollectionUtilities.hasContent(nullList);
        // then
        assertFalse(result, "hasContent should return false for a null collection");
    }

    @Test
    void hasContent_EmptyCollection_ReturnsFalse() {
        // given
        List<String> emptyList = Collections.emptyList();
        Set<Integer> emptySet = Collections.emptySet();
        // when
        boolean resultList = CollectionUtilities.hasContent(emptyList);
        boolean resultSet = CollectionUtilities.hasContent(emptySet);
        // then
        assertFalse(resultList, "hasContent should return false for an empty List");
        assertFalse(resultSet, "hasContent should return false for an empty Set");
    }

    @Test
    void hasContent_NonEmptyCollection_ReturnsTrue() {
        // given
        List<String> list = new ArrayList<>();
        list.add("element");
        Set<Integer> set = new HashSet<>();
        set.add(42);
        // when
        boolean resultList = CollectionUtilities.hasContent(list);
        boolean resultSet = CollectionUtilities.hasContent(set);
        // then
        assertTrue(resultList, "hasContent should return true for a List containing elements");
        assertTrue(resultSet, "hasContent should return true for a Set containing elements");
    }

    @Test
    void hasContent_CollectionWithNullElements_ReturnsTrue() {
        // given
        List<Object> listWithNull = new ArrayList<>();
        // collection size is 1, even though the element is null
        listWithNull.add(null);
        // when
        boolean result = CollectionUtilities.hasContent(listWithNull);
        // then
        assertTrue(result, "hasContent should return true when collection size > 0, even if elements are null");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("hasContentProvider")
    void hasContent_ParameterizedTests(Collection<?> col, boolean expected, String description) {
        // when
        boolean actual = CollectionUtilities.hasContent(col);
        // then
        assertEquals(expected, actual, description);
    }
}
