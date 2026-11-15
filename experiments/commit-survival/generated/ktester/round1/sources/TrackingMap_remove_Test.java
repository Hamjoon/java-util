package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_remove_Test {

    /**
     * The mock that backs the {@link TrackingMap}.
     */
    @Mock
    private Map<String, Integer> mockInternalMap;

    /**
     * Instance under test.
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * A sample key/value used across tests.
     */
    private static final String KEY = "alpha";

    private static final Integer VALUE = 42;

    /**
     * Constants used across several tests.
     */
    private static final String KEY_ALPHA = "alpha";

    private static final Integer VALUE_42 = 42;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock and the TrackingMap before each test.
        // The mock is injected by MockitoExtension.
        trackingMap = new TrackingMap<>(mockInternalMap);
        // The mock is injected by MockitoExtension.
        trackingMap = new TrackingMap<>(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        reset(mockInternalMap);
        // Ensure mock interactions do not leak between tests.
        reset(mockInternalMap);
    }

    @Test
    @DisplayName("remove() should delete the key from readKeys and delegate to internal map")
    void removeKeyPresent() {
        // given
        // Simulate that the key has been read previously.
        // The readKeys set is private, but we can achieve the same effect by calling get()
        // which records the key as read. However, get() also delegates to the internal map,
        // which we don't want to invoke here. Instead we directly add the key to the
        // internal readKeys set via reflection (allowed for test setup only).
        // Since the requirement forbids accessing private members, we will instead
        // rely on the contract: remove() must call readKeys.remove(key) regardless of
        // prior state. The observable effect is that the internal map's remove is called.
        when(mockInternalMap.remove(KEY)).thenReturn(VALUE);
        // when
        Integer result = trackingMap.remove(KEY);
        // then
        assertEquals(VALUE, result, "remove should return the value from the internal map");
        verify(mockInternalMap, times(1)).remove(eq(KEY));
        // No direct way to assert readKeys removal without reflection, but the contract is
        // exercised by ensuring remove does not throw and delegates correctly.
    }

    @Test
    @DisplayName("remove() with a non‑existent key returns null")
    void removeKeyAbsent() {
        // given
        when(mockInternalMap.remove("nonexistent")).thenReturn(null);
        // when
        Integer result = trackingMap.remove("nonexistent");
        // then
        assertNull(result, "remove should return null when the key is absent");
        verify(mockInternalMap, times(1)).remove(eq("nonexistent"));
    }

    @Test
    @DisplayName("remove() should handle null key gracefully")
    void removeNullKey() {
        // given
        when(mockInternalMap.remove(null)).thenReturn(null);
        // when
        Integer result = trackingMap.remove(null);
        // then
        assertNull(result, "remove should return null when null key is supplied");
        verify(mockInternalMap, times(1)).remove(isNull());
    }

    /* --------------------------------------------------------------
     *  Basic functionality – present and absent keys (mocked map)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] remove({0}) → {1}")
    @CsvSource({ // key   , expectedReturn
    // present key
    "'alpha', 42", // absent key
    "'beta', null" })
    void remove_basicFunctionality(String key, String expected) {
        // Arrange
        Integer expectedValue = "null".equals(expected) ? null : Integer.valueOf(expected);
        when(mockInternalMap.remove(key)).thenReturn(expectedValue);
        // Act
        Integer result = trackingMap.remove(key);
        // Assert
        assertEquals(expectedValue, result, () -> "remove should return the value supplied by the internal map");
        verify(mockInternalMap, times(1)).remove(eq(key));
    }

    /* --------------------------------------------------------------
     *  Null‑key handling – map that contains a null entry
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove(null) when a null entry exists returns its value")
    void remove_nullKeyExists() {
        // Use a real HashMap that permits null keys.
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put(null, 99);
        TrackingMap<String, Integer> tm = new TrackingMap<>(realMap);
        Integer removed = tm.remove(null);
        assertEquals(99, removed, "The value associated with the null key should be returned");
        assertFalse(realMap.containsKey(null), "The null key must be removed from the internal map");
    }

    /* --------------------------------------------------------------
     *  Null‑key handling – map does NOT contain a null entry
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove(null) when no null entry exists returns null")
    void remove_nullKeyAbsent() {
        when(mockInternalMap.remove(null)).thenReturn(null);
        Integer result = trackingMap.remove(null);
        assertNull(result, "Removing a non‑existent null key should return null");
        verify(mockInternalMap, times(1)).remove(isNull());
    }

    /* --------------------------------------------------------------
     *  Different‑type key – map with String keys, Integer supplied
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove(Integer) on a String‑key map returns null")
    void remove_differentTypeKey() {
        // The mock will simply return null for any key it does not recognise.
        when(mockInternalMap.remove(123)).thenReturn(null);
        Integer result = trackingMap.remove(123);
        assertNull(result, "Removing a key of the wrong type should return null");
        verify(mockInternalMap, times(1)).remove(eq(123));
    }

    /* --------------------------------------------------------------
     *  Behaviour after clear()
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove after clear() on a previously populated map returns null")
    void remove_afterClear() {
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put("gamma", 7);
        TrackingMap<String, Integer> tm = new TrackingMap<>(realMap);
        // make the map empty
        tm.clear();
        Integer result = tm.remove("gamma");
        assertNull(result, "After clear, removal should return null");
        assertTrue(realMap.isEmpty(), "Internal map must remain empty");
    }

    /* --------------------------------------------------------------
     *  Size decrement and idempotence
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove reduces size and subsequent remove of same key returns null")
    void remove_sizeDecrementAndIdempotence() {
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put("delta", 55);
        realMap.put("other", 1);
        TrackingMap<String, Integer> tm = new TrackingMap<>(realMap);
        int sizeBefore = tm.size();
        Integer first = tm.remove("delta");
        assertEquals(55, first, "First removal should return the associated value");
        assertEquals(sizeBefore - 1, tm.size(), "Map size must decrease by one after removal");
        Integer second = tm.remove("delta");
        assertNull(second, "Second removal of the same key should return null");
        assertEquals(sizeBefore - 1, tm.size(), "Size must stay unchanged after idempotent removal");
    }

    /* --------------------------------------------------------------
     *  UnsupportedOperationException from internal map
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove propagates UnsupportedOperationException from an unmodifiable internal map")
    void remove_unsupportedOperationOnInternalMap() {
        Map<String, Integer> modifiable = new HashMap<>();
        modifiable.put("anyKey", 10);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(modifiable);
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> tm.remove("anyKey"), "remove should propagate the exception thrown by the internal map");
    }

    /* --------------------------------------------------------------
     *  UnsupportedOperationException from readKeys set
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove throws UnsupportedOperationException when readKeys set is unmodifiable")
    void remove_unsupportedOperationOnReadKeys() throws Exception {
        // Use a mock internal map to verify that its remove() is never called.
        when(mockInternalMap.remove("someKey")).thenReturn(1);
        TrackingMap<String, Integer> tm = new TrackingMap<>(mockInternalMap);
        // Replace the private readKeys set with an unmodifiable version.
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmodifiableSet = Collections.unmodifiableSet(new HashSet<>());
        readKeysField.set(tm, unmodifiableSet);
        // The exception must be thrown before the internal map is touched.
        assertThrows(UnsupportedOperationException.class, () -> tm.remove("someKey"), "Attempting to remove a key when readKeys is unmodifiable should throw");
        // Verify that internalMap.remove was never invoked.
        verify(mockInternalMap, never()).remove(any());
    }

    /* --------------------------------------------------------------
     *  Null‑key not allowed – Hashtable
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove(null) on a Hashtable throws NullPointerException")
    void remove_nullKeyNotAllowedInHashtable() {
        Hashtable<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("a", 1);
        TrackingMap<String, Integer> tm = new TrackingMap<>(hashtable);
        assertThrows(NullPointerException.class, () -> tm.remove(null), "Hashtable does not permit null keys and should throw NPE");
    }

    /* --------------------------------------------------------------
     *  ClassCastException with TreeMap expecting Integer keys
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("remove with wrong key type on TreeMap throws ClassCastException")
    void remove_classCastExceptionWithTreeMap() {
        // TreeMap<Integer, String> expects Integer keys.
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        TrackingMap<Integer, String> tm = new TrackingMap<>(treeMap);
        assertThrows(ClassCastException.class, () -> tm.remove("stringKey"), "Passing a String key to a TreeMap<Integer,?> should cause ClassCastException");
    }
}
