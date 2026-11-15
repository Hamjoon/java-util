package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_put_Test {

    /**
     * The TrackingMap under test.
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * Mocked internal map that {@code TrackingMap} delegates to.
     */
    @Mock
    private Map<String, Integer> mockInternalMap;

    /**
     * Helper class to hold a single test case definition.
     */
    private static class PutCase {

        final Object key;

        final Object value;

        final Object expectedReturn;

        final String description;

        PutCase(Object key, Object value, Object expectedReturn, String description) {
            this.key = key;
            this.value = value;
            this.expectedReturn = expectedReturn;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock instance for us (field annotated with @Mock).
        // Initialise the TrackingMap with the mocked internal map.
        trackingMap = new TrackingMap<>(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions with the mock to keep each test isolated.
        reset(mockInternalMap);
    }

    /**
     * Supplies the basic functionality cases (new key, overwrite, nulls, etc.).
     */
    private static Stream<PutCase> basicPutCases() {
        return Stream.of(new PutCase("alpha", 123, null, "Insert a brand‑new key/value pair – first put returns null."), new PutCase("alpha", 456, 123, "Overwrite an existing key – returns previous value."), new PutCase(null, "nullKeyValue", null, "Insert a null key – HashMap permits null keys, returns null."), new PutCase("beta", null, null, "Insert a null value for a non‑null key – returns null."), new PutCase(new String("gamma"), 789, null, "Distinct String instance equal to key – equality based replacement."), new PutCase("delta", 111, null, "Baseline insertion of a never‑used key."), new PutCase("delta", 222, 111, "Second insertion for same key – returns previous value."), new PutCase("epsilon", "first", null, "Insert a key with a String value."), new PutCase("epsilon", "second", "first", "Overwrite the previous String value."), new PutCase("zeta", new ArrayList<>(), null, "Store a mutable object as the value."));
    }

    /**
     * Additional scenarios that involve different key/value types.
     */
    private static Stream<PutCase> additionalPutCases() {
        return Stream.of(new PutCase("alpha", null, 123, "Overwrite existing key with null value – returns old non‑null value."), new PutCase(42, "answer", null, "Non‑String key type (Integer) – generic handling."), new PutCase("beta", new ArrayList<>(), null, "Insert mutable collection as value for key that previously held null."), new PutCase(null, null, null, "Insert null key with null value – both accepted."), new PutCase("gamma", new HashMap<>(), null, "Store newly created mutable map as value."), new PutCase("epsilon", new String("second"), "first", "Overwrite with new String instance equal to previous value."), new PutCase("zeta", "newValue", new ArrayList<>(), "Replace previously stored mutable object with simple String."), new PutCase("delta", new AtomicInteger(5), 222, "Replace Integer value with AtomicInteger."), new PutCase("theta", new Object(), null, "Insert generic Object value."), new PutCase("iota", new java.math.BigDecimal("12345.6789"), null, "Insert BigDecimal value."));
    }

    @Test
    @DisplayName("put delegates to internal map and returns previous value")
    void put_DelegatesAndReturnsPreviousValue() {
        // GIVEN
        String key = "alpha";
        Integer newValue = 42;
        Integer previousValue = 7;
        // Configure the mock to return a previous value when put is called.
        when(mockInternalMap.put(key, newValue)).thenReturn(previousValue);
        // WHEN
        Integer result = trackingMap.put(key, newValue);
        // THEN
        assertEquals(previousValue, result, "The put method should return the value previously associated with the key.");
        // Verify that the internal map's put method was called exactly once with the same arguments.
        verify(mockInternalMap, times(1)).put(key, newValue);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    @DisplayName("put with a new key returns null")
    void put_NewKeyReturnsNull() {
        // GIVEN
        String key = "beta";
        Integer newValue = 99;
        // When a key is not present, Map.put returns null.
        when(mockInternalMap.put(key, newValue)).thenReturn(null);
        // WHEN
        Integer result = trackingMap.put(key, newValue);
        // THEN
        assertNull(result, "Putting a new key should return null.");
        verify(mockInternalMap, times(1)).put(key, newValue);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    @DisplayName("constructor throws IllegalArgumentException when supplied map is null")
    void constructor_NullMap_ThrowsException() {
        // GIVEN / WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage(), "Constructor should reject a null internal map.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicPutCases")
    void put_BasicFunctionality(PutCase pc) {
        // GIVEN – configure mock to return the expected previous value.
        when(mockInternalMap.put(pc.key, pc.value)).thenReturn(pc.expectedReturn);
        // WHEN
        Object result = trackingMap.put(pc.key, pc.value);
        // THEN – verify return value and delegation.
        assertEquals(pc.expectedReturn, result, "Unexpected return value for key=" + pc.key);
        verify(mockInternalMap, times(1)).put(pc.key, pc.value);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalPutCases")
    void put_AdditionalScenarios(PutCase pc) {
        // GIVEN
        when(mockInternalMap.put(pc.key, pc.value)).thenReturn(pc.expectedReturn);
        // WHEN
        Object result = trackingMap.put(pc.key, pc.value);
        // THEN
        assertEquals(pc.expectedReturn, result, "Unexpected return value for key=" + pc.key);
        verify(mockInternalMap, times(1)).put(pc.key, pc.value);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    @DisplayName("put on unmodifiable map throws UnsupportedOperationException")
    void put_UnmodifiableMap_ThrowsUnsupportedOperationException() {
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        TrackingMap<Object, Object> tm = new TrackingMap<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> tm.put("anyKey", "anyValue"), "Unmodifiable map should throw UnsupportedOperationException on put.");
    }

    @Test
    @DisplayName("put null key on ConcurrentHashMap throws NullPointerException")
    void put_ConcurrentHashMap_NullKey_ThrowsNPE() {
        Map<Object, Object> concurrent = new java.util.concurrent.ConcurrentHashMap<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(concurrent);
        assertThrows(NullPointerException.class, () -> tm.put(null, "value"), "ConcurrentHashMap does not allow null keys.");
    }

    @Test
    @DisplayName("put null value on ConcurrentHashMap throws NullPointerException")
    void put_ConcurrentHashMap_NullValue_ThrowsNPE() {
        Map<Object, Object> concurrent = new java.util.concurrent.ConcurrentHashMap<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(concurrent);
        assertThrows(NullPointerException.class, () -> tm.put("key", null), "ConcurrentHashMap does not allow null values.");
    }

    @Test
    @DisplayName("put non‑Comparable key on TreeMap throws ClassCastException")
    void put_TreeMap_NonComparableKey_ThrowsClassCastException() {
        Map<Object, Object> treeMap = new TreeMap<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(treeMap);
        // does not implement Comparable
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> tm.put(nonComparable, "value"), "TreeMap with natural ordering requires Comparable keys.");
    }

    @Test
    @DisplayName("put null key on TreeMap (natural ordering) throws NullPointerException")
    void put_TreeMap_NullKey_ThrowsNPE() {
        Map<Object, Object> treeMap = new TreeMap<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(treeMap);
        assertThrows(NullPointerException.class, () -> tm.put(null, "value"), "TreeMap does not permit null keys with natural ordering.");
    }

    @Test
    @DisplayName("put null key on Hashtable throws NullPointerException")
    void put_Hashtable_NullKey_ThrowsNPE() {
        Map<Object, Object> hashtable = new Hashtable<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(hashtable);
        assertThrows(NullPointerException.class, () -> tm.put(null, "value"), "Hashtable does not allow null keys.");
    }

    @Test
    @DisplayName("put null value on Hashtable throws NullPointerException")
    void put_Hashtable_NullValue_ThrowsNPE() {
        Map<Object, Object> hashtable = new Hashtable<>();
        TrackingMap<Object, Object> tm = new TrackingMap<>(hashtable);
        assertThrows(NullPointerException.class, () -> tm.put("key", null), "Hashtable does not allow null values.");
    }

    @Test
    @DisplayName("put after internalMap set to null via reflection throws NullPointerException")
    void put_InternalMapNull_ThrowsNPE() throws Exception {
        // Use the mock based instance created in @BeforeEach.
        // Force internalMap to null using reflection.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, null);
        assertThrows(NullPointerException.class, () -> trackingMap.put("anyKey", "anyValue"), "Calling put when internalMap is null should throw NullPointerException.");
    }
}
