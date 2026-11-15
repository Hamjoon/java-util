package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_comparator_Test {

    /**
     * Map created with the no‑arg constructor – it should use natural ordering.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> naturalMap;

    /**
     * Map created with an explicit comparator – the same comparator must be returned.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> customMap;

    /**
     * A simple reverse‑order comparator used for the custom map.
     */
    private static final Comparator<String> REVERSE_ORDER = Comparator.<String>reverseOrder();

    @BeforeEach
    void setupBeforeEach() {
        naturalMap = new ConcurrentNavigableMapNullSafe<>();
        customMap = new ConcurrentNavigableMapNullSafe<>(REVERSE_ORDER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid cross‑test contamination.
        naturalMap.clear();
        customMap.clear();
    }

    /**
     * Supplies a stream of distinct comparator instances for the parameterized test.
     */
    private static Stream<Comparator<?>> providedComparatorProvider() {
        Comparator<Integer> descendingInt = Comparator.<Integer>reverseOrder();
        Comparator<String> reverseString = Comparator.<String>reverseOrder();
        Comparator<String> naturalString = String::compareTo;
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        return Stream.of(descendingInt, reverseString, naturalString, lengthComparator);
    }

    @Test
    void comparatorReturnsNullWhenNoComparatorSupplied() {
        // The no‑arg constructor delegates to the constructor that receives a null comparator.
        // According to the contract, comparator() must return null in this case.
        assertNull(naturalMap.comparator(), "Comparator should be null when the map is created without an explicit comparator");
    }

    @Test
    void comparatorReturnsProvidedComparator() {
        // The map was created with REVERSE_ORDER; the same instance must be returned.
        Comparator<? super String> returned = customMap.comparator();
        assertNotNull(returned, "Comparator should not be null when a comparator is supplied");
        assertSame(REVERSE_ORDER, returned, "Comparator returned by comparator() must be the same instance supplied to the constructor");
    }

    @Test
    void comparatorAffectsKeyOrdering() {
        // Populate both maps with the same keys (including a null key).
        naturalMap.put("a", 1);
        naturalMap.put("b", 2);
        naturalMap.put(null, 99);
        customMap.put("a", 1);
        customMap.put("b", 2);
        customMap.put(null, 99);
        // Natural ordering – null key is considered *last*.
        NavigableSet<String> naturalKeys = naturalMap.navigableKeySet();
        assertEquals("a", naturalKeys.first(), "First key in natural order should be 'a'");
        assertEquals("b", naturalKeys.higher("a"), "Second key in natural order should be 'b'");
        assertEquals(null, naturalKeys.last(), "Last key in natural order should be the null key");
        // Custom reverse ordering – null key is considered *first* in the descending view.
        // Since the comparator only defines ordering for non‑null keys, the map still places
        // the null key at the *first* position in the descending view.
        NavigableSet<String> customKeysDesc = customMap.descendingKeySet();
        assertEquals(null, customKeysDesc.first(), "In descending view with custom comparator, null key should be first");
        assertEquals("b", customKeysDesc.higher(null), "Next key after null should be 'b' (reverse order)");
        assertEquals("a", customKeysDesc.last(), "Last key in descending view should be 'a'");
    }

    /**
     * Verify that a map constructed with a natural‑order comparator (i.e. a non‑null
     * Comparator that simply delegates to {@link String#compareTo}) still returns
     * that comparator instance – the implementation must not treat it specially.
     */
    @Test
    void comparatorReturnsNaturalOrderComparatorInstance() {
        Comparator<String> naturalOrderComparator = String::compareTo;
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(naturalOrderComparator);
        Comparator<? super String> returned = map.comparator();
        assertNotNull(returned, "Comparator should not be null when a natural‑order comparator is supplied");
        assertSame(naturalOrderComparator, returned, "The exact comparator instance supplied to the constructor must be returned");
    }

    /**
     * After a series of put/remove operations the map must still expose the original
     * comparator instance.
     */
    @Test
    void comparatorRemainsSameAfterMutations_IntegerKey() {
        Comparator<Integer> ascComparator = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(ascComparator);
        Comparator<? super Integer> before = map.comparator();
        // Perform a mixture of put and remove operations.
        map.put(5, "a");
        map.put(1, "b");
        map.remove(5);
        map.put(3, "c");
        Comparator<? super Integer> after = map.comparator();
        assertSame(before, after, "Comparator instance must remain unchanged after map mutations");
    }

    /**
     * After inserting a null key (handled by the null‑safe wrapper) the comparator
     * instance must still be the one supplied at construction time.
     */
    @Test
    void comparatorRemainsSameAfterNullKeyInsertion() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>(lengthComparator);
        Comparator<? super String> before = map.comparator();
        map.put("aaa", "v1");
        map.put("bb", "v2");
        // null key is allowed by the wrapper
        map.put(null, "v3");
        Comparator<? super String> after = map.comparator();
        assertSame(before, after, "Comparator instance must stay the same after inserting a null key");
    }

    /**
     * Constructing the map with a {@code null} comparator via the constructor that
     * expects a non‑null argument should throw a {@link NullPointerException}.
     */
    @Test
    void constructorRejectsExplicitNullComparator() {
        Executable ctor = () -> new ConcurrentNavigableMapNullSafe<String, Integer>(null);
        assertThrows(NullPointerException.class, ctor, "Providing a null comparator to the explicit‑argument constructor must throw NPE");
    }

    /**
     * Verify that a comparator which throws a {@link RuntimeException} during comparison
     * does not affect the {@code comparator()} method itself, but causes map operations
     * that rely on ordering to propagate the exception.
     */
    @Test
    void comparatorThatThrowsDuringComparePropagatesExceptionOnPut() {
        Comparator<Integer> explodingComparator = (a, b) -> {
            throw new ArithmeticException("division by zero");
        };
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(explodingComparator);
        // comparator() should simply return the stored instance.
        assertSame(explodingComparator, map.comparator(), "comparator() must return the exact instance supplied, even if it is faulty");
        // Attempting to put a key forces the map to compare keys, which should propagate the exception.
        Executable put = () -> map.put(1, "one");
        assertThrows(ArithmeticException.class, put, "Map operations that trigger key comparison must propagate the comparator's exception");
    }

    /**
     * Parameterized test that verifies the map returns the exact comparator instance
     * for a variety of supplied comparators (including natural‑order, reverse‑order,
     * and custom implementations).
     */
    @ParameterizedTest(name = "[{index}] Comparator class: {0}")
    @MethodSource("providedComparatorProvider")
    void comparatorReturnsSuppliedInstance(Comparator<?> comparator) {
        // The generic type is erased at runtime, so we use raw types for construction.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<Object, Integer> map = new ConcurrentNavigableMapNullSafe<>(comparator);
        assertSame(comparator, map.comparator(), "The map must return the same comparator instance that was supplied");
    }
}
