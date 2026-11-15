package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_comparator_Test {

    /**
     * Shared resources that could be reused across tests.
     */
    private static Comparator<String> lengthComparator;

    /**
     * Instance created for each test (when needed).
     */
    private ConcurrentNavigableSetNullSafe<String> set;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a comparator that will be reused in several tests.
        lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default we start with a fresh, empty set using the no‑arg constructor.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to ensure no side‑effects between tests.
        if (set != null) {
            set.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to clean up for this test suite.
        lengthComparator = null;
    }

    /**
     * Supplies comparator instances for the simple cases where we only need to
     * verify that {@code comparator()} returns the exact same instance (or null).
     */
    private static Stream<Arguments> simpleComparatorProvider() {
        Comparator<Integer> reverseInt = Comparator.comparingInt(Integer::intValue).reversed();
        Comparator<String> length = Comparator.comparingInt(String::length);
        Comparator<Object> lambda = (a, b) -> a.toString().compareToIgnoreCase(b.toString());
        Comparator<UUID> uuidComparator = new Comparator<UUID>() {

            @Override
            public int compare(UUID u1, UUID u2) {
                return u1.compareTo(u2);
            }
        };
        Comparator<Object> reverseOrder = Collections.reverseOrder();
        return Stream.of(// originalComparator = null (default constructor)
        Arguments.of(null, "When the set is created with a null comparator, comparator() should return null."), // natural order comparator
        Arguments.of(Comparator.naturalOrder(), "When the set is created with Comparator.naturalOrder(), the same instance must be returned."), // reverse integer comparator
        Arguments.of(reverseInt, "Custom reverse integer comparator should be returned unchanged."), // length comparator for strings
        Arguments.of(length, "Comparator that orders strings by length must be returned unchanged."), // lambda comparator (case‑insensitive string representation)
        Arguments.of(lambda, "Lambda comparator must be the identical instance."), // anonymous UUID comparator
        Arguments.of(uuidComparator, "Anonymous UUID comparator should be returned unchanged."), // Collections.reverseOrder()
        Arguments.of(reverseOrder, "Comparator returned by Collections.reverseOrder() must be the same instance."));
    }

    /**
     * Supplies a comparator together with a collection of elements to be added
     * after construction. This covers the edge case where elements are added
     * later but the stored comparator must remain unchanged.
     */
    private static Stream<Arguments> comparatorWithElementsProvider() {
        Comparator<Integer> reverseInt = Comparator.comparingInt(Integer::intValue).reversed();
        List<Integer> elements = Arrays.asList(5, 1, 3);
        return Stream.of(Arguments.of(reverseInt, elements, "Reverse integer comparator with elements added after construction."));
    }

    @Test
    void comparator_returnsNullWhenDefaultConstructorUsed() {
        // The default constructor uses natural ordering; originalComparator should be null.
        assertNull(set.comparator(), "Comparator should be null when the set is created with the default constructor");
    }

    @Test
    void comparator_returnsProvidedComparator() {
        Comparator<String> custom = Comparator.comparingInt(String::length);
        ConcurrentNavigableSetNullSafe<String> customSet = new ConcurrentNavigableSetNullSafe<>(custom);
        assertSame(custom, customSet.comparator(), "Comparator returned by comparator() must be the same instance supplied to the constructor");
    }

    @Test
    void comparator_isPreservedWhenUsingCollectionAndComparatorConstructor() {
        Collection<String> coll = Arrays.asList("one", "two", "three");
        ConcurrentNavigableSetNullSafe<String> collSet = new ConcurrentNavigableSetNullSafe<>(coll, lengthComparator);
        assertSame(lengthComparator, collSet.comparator(), "Comparator should be the one passed to the (Collection, Comparator) constructor");
    }

    @Test
    void comparatorAffectsOrdering() {
        // Use the length comparator defined in @BeforeAll.
        ConcurrentNavigableSetNullSafe<String> orderedSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // Insert elements of varying length, including null.
        // length 4
        orderedSet.add("kiwi");
        // length 5
        orderedSet.add("apple");
        // length 6
        orderedSet.add("banana");
        // length 0 (treated as shortest)
        orderedSet.add(null);
        // Verify that iterator respects the supplied comparator.
        Iterator<String> it = orderedSet.iterator();
        assertEquals(null, it.next(), "Null (length 0) should be first");
        assertEquals("kiwi", it.next(), "Element with length 4 should follow");
        assertEquals("apple", it.next(), "Element with length 5 should follow");
        assertEquals("banana", it.next(), "Element with length 6 should follow");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    void comparator_isNullWhenUsingCollectionConstructorWithoutComparator() {
        Collection<String> coll = Arrays.asList("a", "b", null);
        ConcurrentNavigableSetNullSafe<String> collSet = new ConcurrentNavigableSetNullSafe<>(coll);
        assertNull(collSet.comparator(), "Comparator should be null when the collection constructor is used without an explicit comparator");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("simpleComparatorProvider")
    void comparator_returnsSuppliedComparatorOrNull(Comparator<?> supplied, String description) {
        // When supplied comparator is null we use the no‑arg constructor (natural ordering).
        ConcurrentNavigableSetNullSafe<?> setUnderTest;
        if (supplied == null) {
            setUnderTest = new ConcurrentNavigableSetNullSafe<>();
        } else {
            // Raw type usage to avoid generic‑type mismatch for the diverse comparators.
            @SuppressWarnings("unchecked")
            ConcurrentNavigableSetNullSafe<Object> rawSet = new ConcurrentNavigableSetNullSafe<>((Comparator<Object>) supplied);
            setUnderTest = rawSet;
        }
        // The method under test.
        Comparator<?> result = setUnderTest.comparator();
        // Assertion: the returned comparator must be the same instance (or null).
        assertSame(supplied, result, "Comparator() should return the exact instance supplied to the constructor.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("comparatorWithElementsProvider")
    void comparator_unchangedAfterAddingElements(Comparator<Integer> supplied, List<Integer> elements, String description) {
        // Create set with supplied comparator.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Integer> setUnderTest = new ConcurrentNavigableSetNullSafe<>((Comparator<Integer>) supplied);
        // Add the provided elements.
        for (Integer e : elements) {
            setUnderTest.add(e);
        }
        // Verify that the comparator reference has not changed.
        assertSame(supplied, setUnderTest.comparator(), "Comparator() must still return the original comparator after elements are added.");
    }

    /**
     * Specific test for the case where a null comparator is used and elements are added.
     * This mirrors the JSON case where {@code originalComparator = null} and we add elements.
     */
    @Test
    void comparator_nullRemainsNullAfterAddingElements() {
        // Default constructor -> natural ordering, comparator is null.
        ConcurrentNavigableSetNullSafe<Integer> setUnderTest = new ConcurrentNavigableSetNullSafe<>();
        // Add some integers.
        setUnderTest.add(10);
        setUnderTest.add(5);
        setUnderTest.add(20);
        // The comparator should still be null.
        assertNull(setUnderTest.comparator(), "Comparator should remain null after adding elements when constructed with no comparator.");
    }
}
