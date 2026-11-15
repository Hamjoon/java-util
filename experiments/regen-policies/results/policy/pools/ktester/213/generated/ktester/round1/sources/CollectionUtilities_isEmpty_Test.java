package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_isEmpty_Test {

    private List<String> list;

    private Set<Integer> set;

    private Set<String> linkedHashSet;

    /**
     * Minimal custom collection that pretends to contain a single element.
     */
    private static class CustomNonEmptyCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            // Return an iterator over a single null element – size is 1.
            return Collections.singletonList(null).iterator();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    /**
     * Minimal custom collection that is truly empty.
     */
    private static class CustomEmptyCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation.
        list = new ArrayList<>();
        set = new HashSet<>();
        linkedHashSet = new LinkedHashSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear collections – not strictly necessary but makes intent clear.
        list.clear();
        set.clear();
        linkedHashSet.clear();
    }

    /**
     * Provides a stream of arguments: the collection to test, the expected
     * boolean result, and a human‑readable description (used as the test name).
     */
    private static Stream<Arguments> provideCollectionsForIsEmpty() {
        return Stream.of(// null reference
        Arguments.of(null, true, "When the collection argument is null, the method should treat it as empty and return true."), // immutable empty collections
        Arguments.of(Collections.emptyList(), true, "An immutable empty List should cause isEmpty to return true."), Arguments.of(Collections.emptySet(), true, "An immutable empty Set should cause isEmpty to return true."), // newly created mutable empty collections
        Arguments.of(new ArrayList<>(), true, "A newly created mutable ArrayList with no elements is empty; method should return true."), Arguments.of(new LinkedHashSet<>(), true, "An empty LinkedHashSet should be recognised as empty."), Arguments.of(new TreeSet<>(), true, "An empty SortedSet (TreeSet) should be recognised as empty."), Arguments.of(new ConcurrentLinkedQueue<>(), true, "An empty concurrent queue should be recognised as empty."), // collections with elements
        Arguments.of(Arrays.asList(1, 2, 3), false, "A List containing elements is not empty; method should return false."), Arguments.of(new HashSet<>(Arrays.asList("a", "b")), false, "A Set with elements is not empty; method should return false."), Arguments.of(Collections.singletonList(null), false, "A List containing a single null element has size 1, therefore isEmpty must return false."), Arguments.of(new ArrayList<>(Arrays.asList(null, null)), false, "A List with multiple null elements is not empty; method should return false."), // wrapper collections around empty collections
        Arguments.of(Collections.synchronizedList(Collections.emptyList()), true, "A synchronized wrapper around an empty List should be treated as empty."), Arguments.of(Collections.unmodifiableList(Collections.emptyList()), true, "An unmodifiable wrapper around an empty List should be treated as empty."), Arguments.of(Collections.emptyList().subList(0, 0), true, "A subList view of an empty List (size 0) should be recognised as empty."), // custom collection implementations
        Arguments.of(new CustomNonEmptyCollection<>(), false, "A custom Collection implementation that reports non‑empty should cause the method to return false."), Arguments.of(new CustomEmptyCollection<>(), true, "A custom Collection implementation that reports empty via isEmpty() should cause the method to return true."));
    }

    @Test
    @DisplayName("isEmpty(null) should return true")
    void testIsEmpty_NullCollection() {
        assertTrue(CollectionUtilities.isEmpty(null), "Calling isEmpty with a null reference must return true");
    }

    @Test
    @DisplayName("isEmpty(empty List) should return true")
    void testIsEmpty_EmptyList() {
        assertTrue(CollectionUtilities.isEmpty(list), "An empty List should be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(empty Set) should return true")
    void testIsEmpty_EmptySet() {
        assertTrue(CollectionUtilities.isEmpty(set), "An empty Set should be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(empty LinkedHashSet) should return true")
    void testIsEmpty_EmptyLinkedHashSet() {
        assertTrue(CollectionUtilities.isEmpty(linkedHashSet), "An empty LinkedHashSet should be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(non‑empty List) should return false")
    void testIsEmpty_NonEmptyList() {
        list.add("element");
        assertFalse(CollectionUtilities.isEmpty(list), "A List containing elements must not be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(non‑empty Set) should return false")
    void testIsEmpty_NonEmptySet() {
        set.add(42);
        assertFalse(CollectionUtilities.isEmpty(set), "A Set containing elements must not be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(non‑empty LinkedHashSet) should return false")
    void testIsEmpty_NonEmptyLinkedHashSet() {
        linkedHashSet.add("value");
        assertFalse(CollectionUtilities.isEmpty(linkedHashSet), "A LinkedHashSet containing elements must not be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(Collections.emptyList()) should return true")
    void testIsEmpty_JavaEmptyListConstant() {
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()), "Java's built‑in emptyList constant must be reported as empty");
    }

    @Test
    @DisplayName("isEmpty(Collections.emptySet()) should return true")
    void testIsEmpty_JavaEmptySetConstant() {
        assertTrue(CollectionUtilities.isEmpty(Collections.emptySet()), "Java's built‑in emptySet constant must be reported as empty");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideCollectionsForIsEmpty")
    @DisplayName("Parameterized isEmpty tests covering all JSON cases")
    void testIsEmpty_VariousCollections(Collection<?> col, boolean expected, String description) {
        assertEquals(expected, CollectionUtilities.isEmpty(col), description);
    }
}
