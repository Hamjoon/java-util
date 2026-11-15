package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getSynchronizedCollection_Test {

    private static Stream<Arguments> provideCollectionsForSynchronization() {
        return Stream.of(// NavigableSet – non‑empty
        arguments("NavigableSet (non‑empty)", new TreeSet<>(Arrays.asList(1, 2, 3)), Collections.synchronizedNavigableSet(new TreeSet<>()).getClass()), // NavigableSet – empty
        arguments("NavigableSet (empty)", new TreeSet<Integer>(), Collections.synchronizedNavigableSet(new TreeSet<>()).getClass()), // SortedSet (non‑NavigableSet) – non‑empty, unmodifiable
        arguments("SortedSet (unmodifiable, non‑empty)", Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList("a", "b"))), Collections.synchronizedSortedSet(new TreeSet<>()).getClass()), // SortedSet (non‑NavigableSet) – empty, unmodifiable
        arguments("SortedSet (unmodifiable, empty)", Collections.unmodifiableSortedSet(new TreeSet<>()), Collections.synchronizedSortedSet(new TreeSet<>()).getClass()), // Set – non‑empty
        arguments("Set (HashSet, non‑empty)", new HashSet<>(Arrays.asList("x", "y", "z")), Collections.synchronizedSet(new HashSet<>()).getClass()), // Set – empty
        arguments("Set (HashSet, empty)", new HashSet<>(), Collections.synchronizedSet(new HashSet<>()).getClass()), // List – non‑empty
        arguments("List (ArrayList, non‑empty)", new ArrayList<>(Arrays.asList(10, 20, 30)), Collections.synchronizedList(new ArrayList<>()).getClass()), // List – empty
        arguments("List (LinkedList, empty)", new LinkedList<>(), Collections.synchronizedList(new LinkedList<>()).getClass()), // General Collection – ArrayDeque non‑empty
        arguments("General Collection (ArrayDeque, non‑empty)", new ArrayDeque<>(Arrays.asList("a", "b")), Collections.synchronizedCollection(new ArrayDeque<>()).getClass()), // General Collection – ArrayDeque empty
        arguments("General Collection (ArrayDeque, empty)", new ArrayDeque<>(), Collections.synchronizedCollection(new ArrayDeque<>()).getClass()));
    }

    @Test
    void shouldWrapNavigableSetWithSynchronizedNavigableSet() {
        NavigableSet<String> original = new ConcurrentSkipListSet<>(Arrays.asList("a", "b"));
        Collection<String> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        // The wrapper class used by Collections.synchronizedNavigableSet is
        // java.util.Collections$SynchronizedNavigableSet
        assertEquals(Collections.synchronizedNavigableSet(original).getClass(), wrapped.getClass(), "Expected a synchronized navigable set wrapper");
        // Verify that modifications are reflected in the original set
        wrapped.add("c");
        assertTrue(original.contains("c"));
    }

    @Test
    void shouldWrapSortedSetWithSynchronizedSortedSetWhenNotNavigable() {
        // Collections.emptySortedSet() implements SortedSet but not NavigableSet
        SortedSet<Integer> original = new TreeSet<>(Arrays.asList(1, 2, 3));
        // Cast to SortedSet to hide the NavigableSet nature for the instanceof check
        SortedSet<Integer> asSorted = (SortedSet<Integer>) (SortedSet<?>) original;
        Collection<Integer> wrapped = CollectionUtilities.getSynchronizedCollection(asSorted);
        assertEquals(Collections.synchronizedSortedSet(asSorted).getClass(), wrapped.getClass(), "Expected a synchronized sorted set wrapper");
        wrapped.add(4);
        assertTrue(original.contains(4));
    }

    @Test
    void shouldWrapSetWithSynchronizedSet() {
        Set<Long> original = new HashSet<>(Arrays.asList(10L, 20L));
        Collection<Long> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        assertEquals(Collections.synchronizedSet(original).getClass(), wrapped.getClass(), "Expected a synchronized set wrapper");
        wrapped.add(30L);
        assertTrue(original.contains(30L));
    }

    @Test
    void shouldWrapListWithSynchronizedList() {
        List<String> original = new ArrayList<>(Arrays.asList("x", "y"));
        Collection<String> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        assertEquals(Collections.synchronizedList(original).getClass(), wrapped.getClass(), "Expected a synchronized list wrapper");
        wrapped.add("z");
        assertTrue(original.contains("z"));
    }

    @Test
    void shouldWrapOtherCollectionWithSynchronizedCollection() {
        // ArrayDeque implements Collection but not List or Set
        Collection<Integer> original = new ArrayDeque<>(Arrays.asList(5, 6, 7));
        Collection<Integer> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        assertEquals(Collections.synchronizedCollection(original).getClass(), wrapped.getClass(), "Expected a generic synchronized collection wrapper");
        wrapped.add(8);
        assertTrue(original.contains(8));
    }

    @Test
    void shouldThrowNPEWhenCollectionIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getSynchronizedCollection(null));
        assertEquals("Collection must not be null", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for each collection type / emptiness variant
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideCollectionsForSynchronization")
    void shouldWrapCollectionWithCorrectSynchronizedView(String description, Collection<?> original, Class<?> expectedWrapperClass) {
        Collection<?> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        // Verify that the wrapper class matches the one returned by the JDK factory method
        assertEquals(expectedWrapperClass, wrapped.getClass(), () -> "Expected wrapper class " + expectedWrapperClass.getName());
        // Verify that the wrapper delegates to the original collection (mutability test)
        // We add a distinct element and check that it appears in the original collection.
        // For collections that do not allow addition (e.g., unmodifiable SortedSet) we skip this step.
        if (original instanceof Set) {
            Object newElement = "newElement";
            wrapped.add(newElement);
            assertTrue(original.contains(newElement), "Modification through wrapper should affect the original collection");
        } else if (original instanceof List) {
            Object newElement = "newElement";
            wrapped.add(newElement);
            assertTrue(original.contains(newElement), "Modification through wrapper should affect the original collection");
        } else if (original instanceof Collection) {
            // For generic collections like ArrayDeque we can also add.
            Object newElement = "newElement";
            wrapped.add(newElement);
            assertTrue(original.contains(newElement), "Modification through wrapper should affect the original collection");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Wrapper mutability verification (explicit separate tests)
     * --------------------------------------------------------------------- */
    @Test
    void wrapperShouldPropagateAdditionsToOriginalList() {
        List<Integer> original = new ArrayList<>(Arrays.asList(1, 2));
        Collection<Integer> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        wrapped.add(3);
        assertTrue(original.contains(3), "Original list should contain element added via wrapper");
    }

    @Test
    void wrapperShouldPropagateRemovalsToOriginalSet() {
        Set<String> original = new HashSet<>(Collections.singleton("alpha"));
        Collection<String> wrapped = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(wrapped.remove("alpha"), "Removal via wrapper should succeed");
        assertTrue(original.isEmpty(), "Original set should be empty after removal through wrapper");
    }

    /* --------------------------------------------------------------------- *
     *  Already synchronized collection input
     * --------------------------------------------------------------------- */
    @Test
    void shouldWrapAlreadySynchronizedCollectionWithNewWrapper() {
        List<Integer> alreadySync = Collections.synchronizedList(new ArrayList<>(Arrays.asList(5, 6)));
        Collection<Integer> wrapped = CollectionUtilities.getSynchronizedCollection(alreadySync);
        // The wrapper class must be the same type (SynchronizedList) but a distinct instance.
        assertEquals(Collections.synchronizedList(new ArrayList<>()).getClass(), wrapped.getClass(), "Expected a synchronized list wrapper");
        assertNotSame(alreadySync, wrapped, "Wrapping an already synchronized collection should produce a new wrapper instance");
    }
}
