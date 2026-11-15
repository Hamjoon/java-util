package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_comparator_Test {

    /**
     * Map created with the default (no‑arg) constructor – uses natural ordering.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> naturalMap;

    /**
     * Map created with an explicit comparator.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> customMap;

    /**
     * A simple comparator used for the {@code customMap} tests.
     */
    private static final Comparator<String> REVERSE_ORDER = Comparator.<String>reverseOrder();

    /**
     * Serializable comparator that orders strings by their length.
     * Used to verify that a serializable comparator instance is returned unchanged.
     */
    private static class SerializableLengthComparator implements Comparator<String>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(String o1, String o2) {
            return Integer.compare(o1.length(), o2.length());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a map that relies on natural ordering (comparator() should be null).
        naturalMap = new ConcurrentNavigableMapNullSafe<>();
        // Initialise a map that uses a user‑supplied comparator.
        customMap = new ConcurrentNavigableMapNullSafe<>(REVERSE_ORDER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid side effects between tests.
        if (naturalMap != null) {
            naturalMap.clear();
        }
        if (customMap != null) {
            customMap.clear();
        }
    }

    /**
     * Supplies a range of comparator instances together with the expected
     * comparator that should be returned by {@code map.comparator()}.
     */
    private static Stream<Arguments> comparatorProvider() {
        // natural order comparator (static instance from Comparator)
        Comparator<String> naturalOrder = Comparator.naturalOrder();
        // reverse order comparator (static instance from Collections)
        Comparator<String> reverseOrder = Collections.reverseOrder();
        // length‑based comparator (anonymous class)
        Comparator<String> lengthComparator = new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return Integer.compare(o1.length(), o2.length());
            }
        };
        // lambda comparator for Integer keys
        Comparator<Integer> lambdaIntComparator = (a, b) -> Integer.compare(a, b);
        // case‑insensitive comparator (pre‑defined constant)
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        // serializable comparator (inner static class defined below)
        Comparator<String> serializableComparator = new SerializableLengthComparator();
        // Explicitly null comparator (passed to constructor)
        Comparator<String> explicitNull = null;
        return Stream.of(// natural order
        Arguments.of("Natural order comparator", naturalOrder, naturalOrder), // reverse order via Collections.reverseOrder()
        Arguments.of("Reverse order comparator", reverseOrder, reverseOrder), // custom length‑based comparator
        Arguments.of("Length‑based comparator (anonymous class)", lengthComparator, lengthComparator), // lambda comparator for Integer keys (using raw map to avoid generic clash)
        Arguments.of("Lambda Integer comparator", lambdaIntComparator, lambdaIntComparator), // case‑insensitive string comparator
        Arguments.of("Case‑insensitive string comparator", caseInsensitive, caseInsensitive), // serializable comparator instance
        Arguments.of("Serializable length comparator", serializableComparator, serializableComparator), // explicit null comparator
        Arguments.of("Explicit null comparator", explicitNull, null));
    }

    @Test
    void comparatorReturnsNullWhenNaturalOrderingIsUsed() {
        // GIVEN – a map constructed with the no‑arg constructor (natural ordering)
        // WHEN
        Comparator<? super String> comparator = naturalMap.comparator();
        // THEN
        assertNull(comparator, "Comparator should be null for a map that uses natural ordering");
    }

    @Test
    void comparatorReturnsSuppliedComparator() {
        // GIVEN – a map constructed with an explicit comparator (reverse order)
        // WHEN
        Comparator<? super String> comparator = customMap.comparator();
        // THEN
        assertNotNull(comparator, "Comparator should not be null when supplied in the constructor");
        assertSame(REVERSE_ORDER, comparator, "Comparator returned by the map must be the exact instance supplied to the constructor");
    }

    @Test
    void comparatorAffectsOrderingAsExpected() {
        // GIVEN – a map with a reverse‑order comparator
        customMap.put("a", 1);
        customMap.put("b", 2);
        customMap.put("c", 3);
        // WHEN – we retrieve the first (lowest) key according to the map's ordering
        String firstKey = customMap.firstKey();
        // THEN – because the comparator reverses natural order, the "largest" key should appear first
        assertEquals("c", firstKey, "With a reverse‑order comparator, the firstKey should be the highest natural key");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("comparatorProvider")
    @SuppressWarnings("unchecked")
    void comparatorReturnsExactlyTheSuppliedInstance(String description, Comparator suppliedComparator, Comparator expectedComparator) {
        // GIVEN – a map constructed with the supplied comparator (may be null)
        ConcurrentNavigableMapNullSafe map;
        if (suppliedComparator == null) {
            // explicit null constructor
            map = new ConcurrentNavigableMapNullSafe<>(null);
        } else {
            map = new ConcurrentNavigableMapNullSafe<>(suppliedComparator);
        }
        // WHEN
        Comparator<?> result = map.comparator();
        // THEN – the returned comparator must be the same instance that was provided
        assertSame(expectedComparator, result, () -> "Comparator returned by map should be the exact instance supplied (" + description + ")");
    }

    @Test
    void subMapViewKeepsOriginalComparator() {
        // GIVEN – a map with a reverse‑order comparator and several entries
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(REVERSE_ORDER);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // WHEN – we obtain a sub‑map view (inclusive range)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", true);
        // THEN – the sub‑map's comparator must be the same reverse‑order instance
        assertSame(REVERSE_ORDER, sub.comparator(), "Sub‑map view should retain the original reverse‑order comparator");
    }
}
