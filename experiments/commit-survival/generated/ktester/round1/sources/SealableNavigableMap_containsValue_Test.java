package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_containsValue_Test {

    /* --------------------------------------------------------------------- */
    private SealableNavigableMap<String, Integer> mapUnderTest;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @Mock
    private NavigableMap<String, Integer> // used for delegation verification
    mockNavMap;

    // Simple custom classes used for equality tests
    static class MyObj {

        final int value;

        MyObj(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MyObj && ((MyObj) o).value == value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    static class Base {

        final int field;

        Base(int f) {
            this.field = f;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Base && ((Base) o).field == field;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field);
        }
    }

    static class SubBase extends Base {

        SubBase(int f) {
            super(f);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sealed flag and supplier for each test
        sealed = false;
        sealedSupplier = () -> sealed;
        // Initialise the mock (MockitoExtension does this automatically)
        // Create a SealableNavigableMap that wraps the mock – this allows us
        // to verify that containsValue() is delegated to the underlying map.
        mapUnderTest = new SealableNavigableMap<>(mockNavMap, sealedSupplier);
    }

    /**
     * Supplies the arguments for {@link #containsValue_basicScenarios}.
     */
    private static Stream<Arguments> basicContainsValueProvider() {
        return Stream.of(// Map contains the value "apple"; should return true.
        Arguments.of("value present – \"apple\"", (Consumer<NavigableMap<String, Object>>) m -> m.put("k1", "apple"), "apple", true, true), // Map does not contain the value "banana"; should return false.
        Arguments.of("value absent – \"banana\"", (Consumer<NavigableMap<String, Object>>) m -> m.put("k1", "apple"), "banana", false, true), // Map has an entry with a null value; containsValue(null) should be true.
        Arguments.of("null value present", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("k1", null);
            m.put("k2", "x");
        }, null, true, true), // Map has no null values; containsValue(null) should be false.
        Arguments.of("null value absent", (Consumer<NavigableMap<String, Object>>) m -> m.put("k1", "x"), null, false, true), // Empty map; containsValue should always return false.
        Arguments.of("empty map – any value", (Consumer<NavigableMap<String, Object>>) m -> {
        }, /* leave empty */
        "any", false, true), // Duplicate values – map contains multiple keys mapping to "duplicate".
        Arguments.of("duplicate values – \"duplicate\"", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("a", "duplicate");
            m.put("b", "duplicate");
        }, "duplicate", true, true), // Different object but equals existing value.
        Arguments.of("different object equal to existing value", (Consumer<NavigableMap<String, Object>>) m -> m.put("k", "apple"), new String("apple"), true, true), // Type mismatch – map stores Strings only, query with Integer.
        Arguments.of("type mismatch – Integer vs String values", (Consumer<NavigableMap<String, Object>>) m -> m.put("k", "one"), 123, false, true), // Size unchanged verification – after calling containsValue.
        Arguments.of("size unchanged after containsValue", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("a", "apple");
            m.put("b", "banana");
        }, "apple", true, true));
    }

    /**
     * Supplies arguments for {@link #containsValue_additionalScenarios}.
     */
    private static Stream<Arguments> additionalContainsValueProvider() {
        return Stream.of(// Two null values present.
        Arguments.of("two null values", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("a", null);
            m.put("b", null);
        }, null, true), // Entry removed before query.
        Arguments.of("value removed before query", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("k", "temp");
            m.remove("k");
        }, // simulate removal
        "temp", false), // Map cleared after population.
        Arguments.of("map cleared", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("x", "any");
            m.clear();
        }, "any", false), // Custom object equality.
        Arguments.of("custom object equality", (Consumer<NavigableMap<String, Object>>) m -> m.put("c", new MyObj(5)), new MyObj(5), true), // Double.NaN handling.
        Arguments.of("Double.NaN equality", (Consumer<NavigableMap<String, Object>>) m -> m.put("d", Double.NaN), Double.NaN, true), // Float.NaN handling.
        Arguments.of("Float.NaN equality", (Consumer<NavigableMap<String, Object>>) m -> m.put("f", Float.NaN), Float.NaN, true), // Boolean.TRUE handling.
        Arguments.of("Boolean.TRUE present", (Consumer<NavigableMap<String, Object>>) m -> m.put("b", Boolean.TRUE), Boolean.TRUE, true), // List equality (different instance, same contents).
        Arguments.of("List equality with same contents", (Consumer<NavigableMap<String, Object>>) m -> m.put("list", Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(1, 2)), true), // Inheritance equality (Base vs SubBase).
        Arguments.of("Base/SubBase equality", (Consumer<NavigableMap<String, Object>>) m -> m.put("sub", new SubBase(10)), new Base(10), true), // Different objects with same hashcode but not equal.
        Arguments.of("hash‑collision but not equal", (Consumer<NavigableMap<String, Object>>) m -> {
            m.put("i", 0);
            m.put("b", false);
        }, // Two distinct objects with same hashcode (e.g., Integer 0 and Boolean false)
        "nonexistent", false));
    }

    @Test
    void containsValue_delegatesToUnderlyingMap_whenUnsealed() {
        // Arrange
        when(mockNavMap.containsValue(42)).thenReturn(true);
        when(mockNavMap.containsValue(99)).thenReturn(false);
        // Act
        boolean resultPresent = mapUnderTest.containsValue(42);
        boolean resultAbsent = mapUnderTest.containsValue(99);
        // Assert
        assertTrue(resultPresent, "Expected true for a value that the underlying map reports as present");
        assertFalse(resultAbsent, "Expected false for a value that the underlying map reports as absent");
        // Verify delegation
        verify(mockNavMap, times(1)).containsValue(42);
        verify(mockNavMap, times(1)).containsValue(99);
    }

    @Test
    void containsValue_delegatesToUnderlyingMap_whenSealed() {
        // Arrange
        // switch to sealed state
        sealed = true;
        when(mockNavMap.containsValue(null)).thenReturn(true);
        when(mockNavMap.containsValue(0)).thenReturn(false);
        // Act
        boolean resultNull = mapUnderTest.containsValue(null);
        boolean resultZero = mapUnderTest.containsValue(0);
        // Assert
        assertTrue(resultNull, "Even when sealed, containsValue(null) should return true if present");
        assertFalse(resultZero, "Even when sealed, containsValue(0) should return false if absent");
        // Verify delegation still occurs
        verify(mockNavMap, times(1)).containsValue(null);
        verify(mockNavMap, times(1)).containsValue(0);
    }

    @Test
    void containsValue_returnsTrueForExistingValue() {
        // Use a real backing map to check functional behaviour (no mocking)
        NavigableMap<String, Integer> realMap = new java.util.TreeMap<>();
        realMap.put("one", 1);
        realMap.put("two", 2);
        // include a null entry
        realMap.put(null, null);
        SealableNavigableMap<String, Integer> realSeal = new SealableNavigableMap<>(realMap, sealedSupplier);
        // Unsealed – normal reads
        assertTrue(realSeal.containsValue(1), "Should find existing integer value 1");
        assertTrue(realSeal.containsValue(null), "Should find null value when present");
        assertFalse(realSeal.containsValue(99), "Should not find a value that is not present");
    }

    @Test
    void containsValue_behavesCorrectlyWhenSealed_usingRealMap() {
        // Arrange a real map with known contents
        NavigableMap<String, Integer> realMap = new java.util.TreeMap<>();
        realMap.put("alpha", 10);
        realMap.put("beta", 20);
        SealableNavigableMap<String, Integer> realSeal = new SealableNavigableMap<>(realMap, sealedSupplier);
        // Seal the map
        sealed = true;
        // Act & Assert – read‑only operation must still work
        assertTrue(realSeal.containsValue(10), "Sealed map should still report present value");
        assertFalse(realSeal.containsValue(30), "Sealed map should correctly report absent value");
    }

    @Test
    void containsValue_handlesDifferentObjectTypesGracefully() {
        // Using the mock to focus on delegation logic only
        when(mockNavMap.containsValue("string")).thenReturn(false);
        when(mockNavMap.containsValue(123)).thenReturn(true);
        assertFalse(mapUnderTest.containsValue("string"), "String value not present should return false");
        assertTrue(mapUnderTest.containsValue(123), "Integer value present should return true");
        verify(mockNavMap).containsValue("string");
        verify(mockNavMap).containsValue(123);
    }

    /**
     * Parameterised test that covers the majority of the basic scenarios
     * described in the JSON test case group {@code containsValue}.
     *
     * Each {@code Arguments} entry contains:
     * <ul>
     *   <li>a short description (used as the test name)</li>
     *   <li>a {@code Consumer<NavigableMap<String,Object>>} that populates the map</li>
     *   <li>the value to query</li>
     *   <li>the expected boolean result (or {@code null} when the expectation is about side‑effects)</li>
     *   <li>a flag indicating whether the map size should stay unchanged after the call</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicContainsValueProvider")
    void containsValue_basicScenarios(String description, Consumer<NavigableMap<String, Object>> mapPopulator, Object query, Boolean expectedResult, boolean sizeUnchanged) {
        // Arrange – build a real backing map and wrap it with SealableNavigableMap
        NavigableMap<String, Object> realMap = new TreeMap<>();
        mapPopulator.accept(realMap);
        SealableNavigableMap<String, Object> seal = new SealableNavigableMap<>(realMap, sealedSupplier);
        int sizeBefore = realMap.size();
        // Act
        boolean actual = seal.containsValue(query);
        // Assert – verify the boolean result when it is defined
        if (expectedResult != null) {
            assertEquals(expectedResult, actual, () -> "containsValue(" + query + ") should return " + expectedResult);
        }
        // Assert – map must not be mutated by a read‑only operation
        if (sizeUnchanged) {
            assertEquals(sizeBefore, realMap.size(), "containsValue must not modify the map size");
        }
    }

    /**
     * Parameterised test covering the richer set of scenarios listed under
     * {@code containsValueAdditional}.  All cases use a real {@link TreeMap}
     * as the backing store.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalContainsValueProvider")
    void containsValue_additionalScenarios(String description, Consumer<NavigableMap<String, Object>> mapPopulator, Object query, Boolean expectedResult) {
        NavigableMap<String, Object> realMap = new TreeMap<>();
        mapPopulator.accept(realMap);
        SealableNavigableMap<String, Object> seal = new SealableNavigableMap<>(realMap, sealedSupplier);
        assertEquals(expectedResult, seal.containsValue(query), () -> "Scenario '" + description + "' expected " + expectedResult);
    }

    @Test
    void containsValue_throws_NullPointerException_whenDelegateIsNull() {
        // Arrange – construct a SealableNavigableMap with a null underlying map
        SealableNavigableMap<String, Object> broken = new SealableNavigableMap<>(null, sealedSupplier);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> broken.containsValue("any"), "Calling containsValue on a SealableNavigableMap with a null delegate should throw NPE");
    }

    @Test
    void containsValue_propagates_ConcurrentModificationException() {
        // Arrange – a mock that throws CME when containsValue("trigger") is invoked
        when(mockNavMap.containsValue("trigger")).thenThrow(new ConcurrentModificationException("simulated"));
        // Act & Assert
        assertThrows(ConcurrentModificationException.class, () -> mapUnderTest.containsValue("trigger"), "containsValue should propagate ConcurrentModificationException from the underlying map");
    }
}
