package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getSynchronizedCollection_Test {

    // -------------------------------------------------------------------------
    private static NavigableSet<Integer> navigableSetSource;

    private static SortedSet<Integer> sortedSetSource;

    private static Set<Integer> hashSetSource;

    private static List<Integer> arrayListSource;

    // not Set/List/SortedSet/NavigableSet
    private static Collection<Integer> dequeSource;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise immutable source collections – they will be copied for each test
        navigableSetSource = new TreeSet<>(Arrays.asList(1, 2, 3));
        // A SortedSet that is *not* a NavigableSet – use an unmodifiable view
        sortedSetSource = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(4, 5, 6)));
        hashSetSource = new HashSet<>(Arrays.asList(7, 8, 9));
        arrayListSource = new ArrayList<>(Arrays.asList(10, 11, 12));
        dequeSource = new ArrayDeque<>(Arrays.asList(13, 14, 15));
    }

    /**
     * Returns true if the class name suggests it is a JDK synchronized wrapper.
     */
    private static boolean isSynchronizedWrapper(Class<?> clazz) {
        return clazz.getName().contains("Synchronized");
    }

    // ------------------- NavigableSet group -------------------
    private static Stream<Arguments> navigableSetProvider() {
        return Stream.of(// non‑empty TreeSet
        Arguments.of(new TreeSet<>(Arrays.asList(3, 1, 2)), false), // empty TreeSet
        Arguments.of(new TreeSet<Integer>(), true));
    }

    // ------------------- SortedSet group -------------------
    private static Stream<Arguments> sortedSetProvider() {
        // mutable backing set + unmodifiable view
        SortedSet<String> mutable = new TreeSet<>(Arrays.asList("b", "a"));
        SortedSet<String> view = Collections.unmodifiableSortedSet(mutable);
        SortedSet<String> emptyMutable = new TreeSet<>();
        SortedSet<String> emptyView = Collections.unmodifiableSortedSet(emptyMutable);
        return Stream.of(Arguments.of(view, mutable, false), Arguments.of(emptyView, emptyMutable, true));
    }

    // ------------------- Set group -------------------
    private static Stream<Arguments> setProvider() {
        return Stream.of(// non‑empty HashSet
        Arguments.of(new HashSet<>(Set.of("x", "y")), false), // empty HashSet
        Arguments.of(new HashSet<String>(), true));
    }

    // ------------------- List group -------------------
    private static Stream<Arguments> listProvider() {
        return Stream.of(// non‑empty ArrayList
        Arguments.of(new ArrayList<>(List.of(10, 20, 30)), false), // empty ArrayList
        Arguments.of(new ArrayList<Integer>(), true));
    }

    // ------------------- Other Collection group -------------------
    private static Stream<Arguments> otherCollectionProvider() {
        return Stream.of(// non‑empty ArrayDeque
        Arguments.of(new ArrayDeque<>(Arrays.asList("a", "b")), false), // empty ArrayDeque
        Arguments.of(new ArrayDeque<String>(), true));
    }

    @Test
    void getSynchronizedCollection_NullArgument_ThrowsNPE() {
        Collection<Integer> nullCollection = null;
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getSynchronizedCollection(nullCollection));
        assertEquals("Collection must not be null", ex.getMessage(), "Exception message should match the contract");
    }

    @Test
    void getSynchronizedCollection_NavigableSet_ReturnsSynchronizedNavigableSet() {
        // WHEN
        Collection<Integer> result = CollectionUtilities.getSynchronizedCollection(source);
        // THEN – type specific assertions
        assertTrue(result instanceof NavigableSet, "Result must still implement NavigableSet");
        assertTrue(isSynchronizedWrapper(result.getClass()), "Result class should be a synchronized wrapper");
        assertNotSame(source, result, "Wrapper must be a distinct instance");
        // THEN – content parity
        assertEquals(expectEmpty, result.isEmpty(), "Wrapper emptiness must match the source collection");
        assertTrue(result.containsAll(source) && source.containsAll(result), "Wrapper must contain exactly the same elements as the source");
        // THEN – mutation reflection (only for mutable source)
        if (!expectEmpty) {
            source.add(99);
            assertTrue(((NavigableSet<Integer>) result).contains(99), "Wrapper must reflect modifications made to the underlying collection");
        }
    }

    @Test
    void getSynchronizedCollection_SortedSet_ReturnsSynchronizedSortedSet() {
        // WHEN
        Collection<String> result = CollectionUtilities.getSynchronizedCollection(sourceView);
        // THEN – wrapper type
        assertTrue(result instanceof SortedSet, "Result must still implement SortedSet");
        assertTrue(isSynchronizedWrapper(result.getClass()), "Result class should be a synchronized wrapper");
        assertNotSame(sourceView, result, "Wrapper must be a distinct instance");
        // THEN – content parity
        assertEquals(expectEmpty, result.isEmpty(), "Wrapper emptiness must match the source view");
        assertTrue(result.containsAll(sourceView) && sourceView.containsAll(result), "Wrapper must contain exactly the same elements as the source view");
        // THEN – mutation reflection via the mutable backing set
        mutableBacking.add("c");
        assertTrue(((SortedSet<String>) result).contains("c"), "Wrapper must reflect changes made to the underlying mutable set");
    }

    @Test
    void getSynchronizedCollection_Set_ReturnsSynchronizedSet() {
        // WHEN
        Collection<String> result = CollectionUtilities.getSynchronizedCollection(source);
        // THEN – wrapper specifics
        assertTrue(result instanceof Set, "Result must still implement Set");
        assertTrue(isSynchronizedWrapper(result.getClass()), "Result should be a synchronized wrapper");
        assertNotSame(source, result, "Wrapper must be a distinct instance");
        // THEN – content parity
        assertEquals(expectEmpty, result.isEmpty(), "Wrapper emptiness must match source");
        assertTrue(result.containsAll(source) && source.containsAll(result), "Wrapper must contain exactly the same elements as the source");
        // THEN – mutation reflection
        ((Set<String>) result).add("z");
        assertTrue(source.contains("z"), "Underlying set must reflect changes made via wrapper");
    }

    @Test
    void getSynchronizedCollection_List_ReturnsSynchronizedList() {
        // WHEN
        Collection<Integer> result = CollectionUtilities.getSynchronizedCollection(source);
        // THEN – wrapper specifics
        assertTrue(result instanceof List, "Result must still implement List");
        assertTrue(isSynchronizedWrapper(result.getClass()), "Result should be a synchronized wrapper");
        assertNotSame(source, result, "Wrapper must be a distinct instance");
        // THEN – content parity
        assertEquals(expectEmpty, result.isEmpty(), "Wrapper emptiness must match source");
        assertTrue(result.containsAll(source) && source.containsAll(result), "Wrapper must contain exactly the same elements as the source");
        // THEN – mutation reflection
        ((List<Integer>) result).add(77);
        assertTrue(source.contains(77), "Underlying list must reflect changes made via wrapper");
    }

    @Test
    void getSynchronizedCollection_OtherCollection_ReturnsSynchronizedCollection() {
        // WHEN
        Collection<String> result = CollectionUtilities.getSynchronizedCollection(source);
        // THEN – wrapper specifics (generic synchronized collection)
        assertFalse(result instanceof Set, "Result should not be a Set");
        assertFalse(result instanceof List, "Result should not be a List");
        assertFalse(result instanceof SortedSet, "Result should not be a SortedSet");
        assertFalse(result instanceof NavigableSet, "Result should not be a NavigableSet");
        assertTrue(isSynchronizedWrapper(result.getClass()), "Result should be a synchronized wrapper");
        assertNotSame(source, result, "Wrapper must be a distinct instance");
        // THEN – content parity
        assertEquals(expectEmpty, result.isEmpty(), "Wrapper emptiness must match source");
        assertTrue(result.containsAll(source) && source.containsAll(result), "Wrapper must contain exactly the same elements as the source");
        // THEN – mutation reflection
        result.add("c");
        assertTrue(source.contains("c"), "Underlying collection must reflect changes made via wrapper");
    }
}
