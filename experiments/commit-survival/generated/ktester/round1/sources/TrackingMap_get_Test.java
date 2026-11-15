package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_get_Test {

    @Mock
    private Map<String, Integer> // dependency injected by Mockito
    mockMap;

    // class under test
    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Construct the TrackingMap with the mocked internal map
        trackingMap = new TrackingMap<>(mockMap);
    }

    @Test
    void get_ExistingKey_ReturnsValueAndTracksKey() {
        // given
        String key = "existing";
        Integer expectedValue = 42;
        when(mockMap.get(key)).thenReturn(expectedValue);
        // when
        Integer actual = trackingMap.get(key);
        // then
        assertEquals(expectedValue, actual, "The value returned by get() should match the underlying map");
        verify(mockMap, times(1)).get(key);
        assertTrue(trackingMap.keysUsed().contains(key), "The accessed key should be recorded in keysUsed()");
    }

    @Test
    void get_NonExistingKey_ReturnsNullAndTracksKey() {
        // given
        String key = "missing";
        when(mockMap.get(key)).thenReturn(null);
        // when
        Integer actual = trackingMap.get(key);
        // then
        assertNull(actual, "get() should return null for a missing key");
        verify(mockMap, times(1)).get(key);
        assertTrue(trackingMap.keysUsed().contains(key), "Even a missing key should be recorded in keysUsed()");
    }

    @Test
    void get_NullKey_ReturnsNullAndTracksNullKey() {
        // given
        when(mockMap.get(null)).thenReturn(null);
        // when
        Integer actual = trackingMap.get(null);
        // then
        assertNull(actual, "get(null) should return null when the underlying map returns null");
        verify(mockMap, times(1)).get(null);
        assertTrue(trackingMap.keysUsed().contains(null), "A null key access should be recorded in keysUsed()");
    }

    /* --------------------------------------------------------------
     * Null value handling (key present with explicit null value)
     * -------------------------------------------------------------- */
    @Test
    void get_KeyWithExplicitNullValue_ReturnsNullAndTracksKey() {
        // given
        String key = "nullKey";
        // simulate explicit null entry
        when(mockMap.get(key)).thenReturn(null);
        // (Mockito does not differentiate between missing and explicit null)
        // when
        Integer actual = trackingMap.get(key);
        // then
        assertNull(actual, "Explicit null value should be returned as null");
        assertTrue(trackingMap.keysUsed().contains(key), "Key with null value must be tracked");
    }

    /* --------------------------------------------------------------
     * Duplicate reads – readKeys must not contain duplicates
     * -------------------------------------------------------------- */
    @Test
    void get_DuplicateReads_DoNotDuplicateReadKeys() {
        // given
        String key = "alpha";
        Integer value = 55;
        when(mockMap.get(key)).thenReturn(value);
        // first call
        Integer first = trackingMap.get(key);
        // second call
        Integer second = trackingMap.get(key);
        // then
        assertEquals(value, first);
        assertEquals(value, second);
        assertEquals(1, trackingMap.keysUsed().size(), "Read‑keys set should contain only one entry after duplicate reads");
        assertTrue(trackingMap.keysUsed().contains(key));
    }

    /* --------------------------------------------------------------
     * ClassCastException for incompatible key type
     * -------------------------------------------------------------- */
    @Test
    void get_IncompatibleKeyType_ThrowsClassCastException() {
        // given a TrackingMap<String,Integer>
        // when an Integer key is supplied, the cast to (K) fails
        assertThrows(ClassCastException.class, () -> trackingMap.get(5), "Using an Integer key on a TrackingMap<String,?> must throw ClassCastException");
    }

    /* --------------------------------------------------------------
     * Subclass key type compatibility (Number -> Double)
     * -------------------------------------------------------------- */
    @Test
    void get_SubclassKeyType_Succeeds() {
        // Build a concrete TrackingMap<Number,String>
        Map<Number, String> internal = new HashMap<>();
        internal.put(3.14, "pi");
        TrackingMap<Number, String> numberMap = new TrackingMap<>(internal);
        // when
        String result = numberMap.get(3.14);
        // then
        assertEquals("pi", result);
        assertTrue(numberMap.keysUsed().contains(3.14));
    }

    /* --------------------------------------------------------------
     * Empty map returns null but still tracks key
     * -------------------------------------------------------------- */
    @Test
    void get_EmptyMap_ReturnsNullAndTracksKey() {
        // empty mock (default behaviour is null)
        String key = "anyKey";
        Integer actual = trackingMap.get(key);
        assertNull(actual);
        assertTrue(trackingMap.keysUsed().contains(key));
    }

    /* --------------------------------------------------------------
     * internalMap is null – should throw NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void get_NullInternalMap_ThrowsNullPointerException() {
        TrackingMap<String, Integer> mapWithNullInternal = new TrackingMap<>(null);
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.get("anyKey"), "Calling get() when internalMap is null must throw NPE");
    }

    /* --------------------------------------------------------------
     * readKeys set is null – NullPointerException when adding
     * -------------------------------------------------------------- */
    @Test
    void get_NullReadKeys_ThrowsNullPointerException() throws Exception {
        // use reflection to set the private readKeys field to null
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        assertThrows(NullPointerException.class, () -> trackingMap.get("alpha"), "If readKeys is null, get() should throw NPE when trying to add");
    }

    /* --------------------------------------------------------------
     * Unmodifiable readKeys set – UnsupportedOperationException
     * -------------------------------------------------------------- */
    @Test
    void get_UnmodifiableReadKeys_ThrowsUnsupportedOperationException() throws Exception {
        // replace readKeys with an unmodifiable set via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmod = Collections.unmodifiableSet(new HashSet<>());
        readKeysField.set(trackingMap, unmod);
        assertThrows(UnsupportedOperationException.class, () -> trackingMap.get("beta"), "Adding to an unmodifiable readKeys set must throw UOE");
    }

    /* --------------------------------------------------------------
     * TreeSet readKeys does not allow null – NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void get_TreeSetReadKeysWithNullKey_ThrowsNullPointerException() throws Exception {
        // set readKeys to a TreeSet (no nulls allowed)
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, new TreeSet<>());
        assertThrows(NullPointerException.class, () -> trackingMap.get(null), "TreeSet readKeys.add(null) must throw NPE");
    }

    /* --------------------------------------------------------------
     * internalMap type mismatch – ClassCastException from underlying map
     * -------------------------------------------------------------- */
    @Test
    void get_InternalMapTypeMismatch_ThrowsClassCastException() {
        // internal map expects Integer keys, but TrackingMap is declared with String keys
        Map<Integer, String> mismatched = new TreeMap<>();
        mismatched.put(1, "one");
        @SuppressWarnings("unchecked")
        TrackingMap<String, String> map = new TrackingMap((Map) mismatched);
        assertThrows(ClassCastException.class, () -> map.get("stringKey"), "Calling get with wrong key type on underlying map should throw CCE");
    }

    /* --------------------------------------------------------------
     * internalMap TreeMap does not allow null key – NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void get_InternalTreeMapNullKey_ThrowsNullPointerException() {
        // TreeMap does not permit null keys
        Map<String, Integer> treeMap = new TreeMap<>();
        TrackingMap<String, Integer> map = new TrackingMap<>(treeMap);
        assertThrows(NullPointerException.class, () -> map.get(null), "TreeMap.get(null) should throw NPE before tracking");
    }
}
