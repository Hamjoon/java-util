package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_comparator_Test {

    /**
     * A map created with the default (no‑comparator) constructor.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> defaultMap;

    /**
     * A map created with an explicit comparator.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> customMap;

    /**
     * A simple reverse‑order comparator used for the custom map.
     */
    private static final Comparator<String> REVERSE_ORDER = Comparator.reverseOrder();

    /**
     * Map created with a reverse‑order comparator for {@code String}.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> reverseStringMap;

    /**
     * Simple holder for a comparator and a description used by the
     * {@link #customComparatorProvider()} method.
     */
    private static final class ComparatorCase<K> {

        final Comparator<K> comparator;

        final String description;

        ComparatorCase(Comparator<K> comparator, String description) {
            this.comparator = comparator;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test method.
        defaultMap = new ConcurrentNavigableMapNullSafe<>();
        reverseStringMap = new ConcurrentNavigableMapNullSafe<>(REVERSE_ORDER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the pattern clear.
        defaultMap = null;
        customMap = null;
    }

    private static Stream<ComparatorCase<?>> customComparatorProvider() {
        return Stream.of(// Integer reverse order
        new ComparatorCase<>(Comparator.<Integer>reverseOrder(), "Integer reverse order comparator"), // String length comparator (shorter strings first)
        new ComparatorCase<>(Comparator.comparingInt(String::length), "String length comparator (shorter first)"));
    }

    @Test
    @DisplayName("comparator() returns null when map is created with default constructor")
    void testComparatorIsNullForDefaultConstructor() {
        // GIVEN – a map created via the no‑arg constructor (already set up in @BeforeEach)
        // WHEN
        Comparator<? super String> result = defaultMap.comparator();
        // THEN
        assertNull(result, "The comparator should be null when no comparator is supplied");
    }

    @Test
    @DisplayName("comparator() returns the exact comparator supplied to the constructor")
    void testComparatorReturnsSuppliedInstance() {
        // GIVEN – a map created with a specific comparator (already set up in @BeforeEach)
        // WHEN
        Comparator<? super String> result = customMap.comparator();
        // THEN
        assertNotNull(result, "The comparator should not be null when supplied");
        assertSame(REVERSE_ORDER, result, "The comparator returned must be the same instance that was passed to the constructor");
    }

    @Test
    @DisplayName("custom comparator influences map ordering as expected")
    void testComparatorAffectsOrdering() {
        // GIVEN
        customMap.put("a", 1);
        customMap.put("b", 2);
        customMap.put("c", 3);
        // Null keys are placed first in the descending view; here we only test natural view.
        customMap.put(null, 0);
        // WHEN – retrieve the first (lowest) key according to the reverse order comparator
        // reverse order → highest natural key becomes first
        String firstKey = customMap.firstKey();
        // THEN
        assertEquals("c", firstKey, "With a reverse‑order comparator, the highest natural key should be reported as the first key");
        // Verify that the comparator instance is still the one we supplied
        assertSame(REVERSE_ORDER, customMap.comparator(), "Comparator instance must remain unchanged after map mutations");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("customComparatorProvider")
    @DisplayName("comparator() returns the exact custom comparator supplied at construction")
    <K> void testCustomComparatorIdentity(ComparatorCase<K> caseData) {
        // GIVEN – construct a map with the supplied comparator
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<K, String> map = new ConcurrentNavigableMapNullSafe<>(caseData.comparator);
        // WHEN
        Comparator<? super K> returned = map.comparator();
        // THEN
        assertNotNull(returned, "Comparator should not be null for custom comparator");
        assertSame(caseData.comparator, returned, "Returned comparator must be the exact instance supplied to the constructor");
    }

    @Test
    @DisplayName("comparator() remains identical after a series of map mutations")
    void testComparatorStabilityAfterMutations() {
        // GIVEN – map with reverse integer comparator
        Comparator<Integer> reverseInt = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(reverseInt);
        Comparator<? super Integer> captured = map.comparator();
        // Perform a variety of mutations
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.remove(2);
        map.clear();
        map.put(5, "five");
        // WHEN – retrieve comparator again
        Comparator<? super Integer> after = map.comparator();
        // THEN – should be the same instance as before
        assertSame(captured, after, "Comparator instance must stay the same after map mutations");
    }

    @Test
    @DisplayName("comparator() safely returns null when underlying map is null")
    void testComparatorWithNullUnderlyingMap() throws Exception {
        // The class has a protected constructor that accepts (ConcurrentNavigableMap, Comparator).
        // We locate it via reflection and invoke it with null arguments.
        Constructor<ConcurrentNavigableMapNullSafe> ctor = null;
        for (Constructor<?> c : ConcurrentNavigableMapNullSafe.class.getDeclaredConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == 2 && ConcurrentNavigableMap.class.isAssignableFrom(params[0]) && Comparator.class.isAssignableFrom(params[1])) {
                //noinspection unchecked
                ctor = (Constructor<ConcurrentNavigableMapNullSafe>) c;
                break;
            }
        }
        // If such a constructor does not exist, fall back to the default constructor (which also yields null).
        ConcurrentNavigableMapNullSafe<Integer, String> map;
        if (ctor != null) {
            ctor.setAccessible(true);
            map = ctor.newInstance(null, null);
        } else {
            // Default constructor already sets originalComparator to null.
            map = new ConcurrentNavigableMapNullSafe<>();
        }
        // WHEN
        Comparator<? super Integer> result = map.comparator();
        // THEN – must be null and no exception should be thrown
        assertNull(result, "Comparator should be null when underlying map is null");
    }
}
